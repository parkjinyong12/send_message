import os
from typing import Optional

from slack_sdk.errors import SlackApiError
from slack_sdk.web.async_client import AsyncWebClient


_client: Optional[AsyncWebClient] = None


def get_slack_token() -> str:
    token = os.getenv("SLACK_BOT_TOKEN", "").strip()
    if not token:
        raise RuntimeError(
            "SLACK_BOT_TOKEN 환경 변수가 설정되지 않았습니다. .env 또는 배포 환경 변수로 설정하세요."
        )
    return token


def get_client() -> AsyncWebClient:
    global _client
    if _client is None:
        _client = AsyncWebClient(token=get_slack_token())
    return _client


async def resolve_channel_id(channel_or_name: str) -> str:
    """
    전달된 값이 채널 ID로 보이면 그대로 사용하고,
    아닌 경우(예: "general" 또는 "#general") Slack API로 채널 ID를 조회합니다.

    참고: 채널 이름 조회에는 추가 권한이 필요할 수 있습니다 (channels:read, groups:read).
    배포 환경에서 가능하면 채널 ID 사용을 권장합니다.
    """
    value = (channel_or_name or "").strip()
    if not value:
        raise ValueError("채널명이 비어있습니다.")

    # 대략적인 채널 ID 패턴: C/G(공개/비공개)로 시작하는 9~11자리 대문자/숫자
    # 정확한 검증은 Slack 문서 참고. 여기서는 간단히 접두로 식별.
    if value[0:1] in {"C", "G"} and len(value) >= 8:
        return value

    normalized = value.lstrip("#")
    client = get_client()

    cursor: Optional[str] = None
    while True:
        resp = await client.conversations_list(
            limit=1000, types="public_channel,private_channel", cursor=cursor
        )
        channels = resp.get("channels", [])
        for ch in channels:
            if ch.get("name") == normalized:
                return ch.get("id")
        cursor = resp.get("response_metadata", {}).get("next_cursor")
        if not cursor:
            break

    # 찾지 못하면 원본 값을 그대로 사용 (Slack이 해석할 수 있으면 성공)
    return value


async def send_slack_message(channel_or_name: str, text: str) -> dict:
    if not text or not text.strip():
        raise ValueError("메시지 내용이 비어있습니다.")

    client = get_client()
    channel = await resolve_channel_id(channel_or_name)
    try:
        result = await client.chat_postMessage(channel=channel, text=text)
        return {
            "ok": True,
            "channel": result.get("channel"),
            "ts": result.get("ts"),
        }
    except SlackApiError as e:
        detail = getattr(e, "response", None)
        error_msg = detail.get("error") if isinstance(detail, dict) else str(e)
        return {"ok": False, "error": error_msg or "Slack API 오류"} 
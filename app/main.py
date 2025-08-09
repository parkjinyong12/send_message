import os
from typing import Any, Dict

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv

from app.slack_service import send_slack_message, get_slack_token

# 개발 환경에서 .env 자동 로드 (배포 환경에서는 외부에서 주입)
load_dotenv()

app = FastAPI(title="Slack Sender API", version="1.0.0")


class MessageRequest(BaseModel):
    channel: str
    text: str


@app.get("/health")
async def health() -> Dict[str, Any]:
    return {"status": "ok"}


@app.post("/send")
async def send(req: MessageRequest) -> Dict[str, Any]:
    try:
        # 토큰 유효성 우선 확인
        _ = get_slack_token()
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))

    try:
        result = await send_slack_message(req.channel, req.text)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:  # 예기치 못한 오류 보호
        raise HTTPException(status_code=500, detail="메시지 전송 중 오류가 발생했습니다.")

    if not result.get("ok"):
        raise HTTPException(status_code=502, detail=result.get("error", "전송 실패"))

    return result


# 개발 편의를 위한 엔트리포인트
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app", host="0.0.0.0", port=int(os.getenv("PORT", "8000")), reload=True
    ) 
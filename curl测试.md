# 1. 用户登录
curl -X POST "http://localhost:8123/api/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "userAccount": "Habby_lele",
    "userPassword": "1234567890"
  }' \
  -c cookies.txt

# 2. 调用生成代码接口（流式）
curl -G "http://localhost:8123/api/app/chat/gen/code" \
  --data-urlencode "appId=361789276682240000" \
  --data-urlencode "message=做一个任务记录，代码不超过20行" \
  -H "Accept: text/event-stream" \
  -H "Cache-Control: no-cache" \
  -b cookies.txt \
  --no-buffer
# Release Readiness Execution
Date: 2026-04-25T15:04:34+08:00
Commit: 86f47b8

## Git Status
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/auth/service/AuthService.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/auth/service/DemoUserInitializer.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/vo/DiagnosisResultDetailVO.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/training/controller/TrainingPlanController.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/training/controller/TrainingSessionController.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/training/dto/StartTrainingSessionRequest.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/training/service/TrainingPlanService.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/training/vo/WrongBookItemVO.java
 M app-server/src/main/java/com/huashi/eftransfer/app/modules/user/service/UserQueryService.java
 M postcss.config.js
 M src/components/common/index.tsx
 M tailwind.config.js
 M tmp/import/semantic-lexicon-v2-production-merged.csv
 M tmp/import/semantic-lexicon-v2-production-remaining-2.csv
?? app-server/src/main/java/com/huashi/eftransfer/app/modules/user/service/DisplayNameNormalizer.java
?? qa-output/

## Docker Status
NAME                     IMAGE                       COMMAND                  SERVICE      CREATED          STATUS                     PORTS
ef-transfer-ai-gateway   ef-transfer-ai-gateway      "java -jar /app/app.…"   ai-gateway   11 minutes ago   Up 11 minutes (healthy)    0.0.0.0:8090->8090/tcp, [::]:8090->8090/tcp
ef-transfer-app-server   ef-transfer-app-server      "java -jar /app/app.…"   app-server   11 minutes ago   Up 10 minutes (healthy)    0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
ef-transfer-frontend     ef-transfer-frontend        "docker-entrypoint.s…"   frontend     4 hours ago      Up 9 minutes (unhealthy)   0.0.0.0:3000->3000/tcp, [::]:3000->3000/tcp
ef-transfer-mysql        mysql:8.4.8                 "docker-entrypoint.s…"   mysql        22 hours ago     Up 4 hours (healthy)       0.0.0.0:3306->3306/tcp, [::]:3306->3306/tcp
ef-transfer-postgres     ef-transfer-postgres        "docker-entrypoint.s…"   postgres     11 minutes ago   Up 11 minutes (healthy)    0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp
ef-transfer-rabbitmq     rabbitmq:4.2.5-management   "docker-entrypoint.s…"   rabbitmq     22 hours ago     Up 4 hours (healthy)       0.0.0.0:5672->5672/tcp, [::]:5672->5672/tcp, 0.0.0.0:15672->15672/tcp, [::]:15672->15672/tcp
ef-transfer-redis        redis:8-bookworm            "docker-entrypoint.s…"   redis        22 hours ago     Up 4 hours (healthy)       127.0.0.1:6379->6379/tcp
\n## Final Docker Snapshot
NAME                     IMAGE                       COMMAND                  SERVICE      CREATED         STATUS                      PORTS
ef-transfer-ai-gateway   ef-transfer-ai-gateway      "java -jar /app/app.…"   ai-gateway   3 minutes ago   Up 2 minutes (healthy)      0.0.0.0:8090->8090/tcp, [::]:8090->8090/tcp
ef-transfer-app-server   ef-transfer-app-server      "java -jar /app/app.…"   app-server   2 minutes ago   Up 2 minutes (healthy)      0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
ef-transfer-frontend     ef-transfer-frontend        "docker-entrypoint.s…"   frontend     4 hours ago     Up 37 minutes (unhealthy)   0.0.0.0:3000->3000/tcp, [::]:3000->3000/tcp
ef-transfer-mysql        mysql:8.4.8                 "docker-entrypoint.s…"   mysql        22 hours ago    Up 4 hours (healthy)        0.0.0.0:3306->3306/tcp, [::]:3306->3306/tcp
ef-transfer-postgres     ef-transfer-postgres        "docker-entrypoint.s…"   postgres     3 minutes ago   Up 2 minutes (healthy)      0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp
ef-transfer-rabbitmq     rabbitmq:4.2.5-management   "docker-entrypoint.s…"   rabbitmq     22 hours ago    Up 4 hours (healthy)        0.0.0.0:5672->5672/tcp, [::]:5672->5672/tcp, 0.0.0.0:15672->15672/tcp, [::]:15672->15672/tcp
ef-transfer-redis        redis:8-bookworm            "docker-entrypoint.s…"   redis        22 hours ago    Up 4 hours (healthy)        127.0.0.1:6379->6379/tcp
\n## API probes after display-name fix
teacher.zhang 200 张老师 TEACHER
student.li 200 李华 STUDENT
student.wang 200 王敏 STUDENT
admin 401 {"success":false,"code":"INVALID_CREDENTIALS","message":"Invalid username/email or password","data":null,"timestamp":"2026-04-25T07:32:03.131462847Z","traceId":
\n## AI Gateway health direct
HTTP/1.1 500 
X-Trace-Id: 0c8ef946e76c1a14fb438a0224d07a3d
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sat, 25 Apr 2026 07:32:03 GMT
Connection: close

{"success":false,"code":"INTERNAL_ERROR","message":"Unexpected error while handling request /actuator/health","data":null,"timestamp":"2026-04-25T07:32:03.174416423Z","traceId":"0c8ef946e76c1a14fb438a0224d07a3d"}
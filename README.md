# RouteSMS

안드로이드 기기에서 수신되는 모든 메시지(SMS, MMS, RCS)를 Slack 채널로 자동 전달하는 앱입니다.

기업 공용 휴대전화의 인증번호 등을 팀원들이 Slack에서 실시간으로 확인할 수 있도록 설계되었습니다.

## 주요 기능

- **SMS/MMS 수신 전달** - BroadcastReceiver를 통한 SMS/MMS 실시간 감지 및 Slack 전송
- **RCS 메시지 대응** - NotificationListenerService를 통한 RCS 및 기타 메시징 앱 알림 캡처
- **중복 방지** - sender/content 기반 이중 캐시로 동일 메시지 중복 전송 방지
- **Foreground Service** - 앱 종료 후에도 안정적으로 메시지 수신
- **실시간 메시지 로그** - 최근 전달된 메시지 50건을 앱 내에서 확인

## 기술 스택

| 항목 | 버전 |
|---|---|
| Kotlin | 2.1.0 |
| Gradle | 8.11.1 |
| AGP | 8.7.3 |
| compileSdk / targetSdk | 35 (Android 15) |
| minSdk | 23 (Android 6.0) |
| UI | Jetpack Compose + Material3 |
| 비동기 처리 | Kotlin Coroutines + WorkManager |
| 네트워크 | Retrofit2 + OkHttp4 |
| 설정 저장 | DataStore Preferences |

## 프로젝트 구조

```
app/src/main/java/com/routesms/
├── RouteSmsApplication.kt          # Application - 알림 채널 생성
├── bind/
│   └── MainActivity.kt             # Compose UI 진입점
├── data/
│   ├── SettingsDataStore.kt        # Webhook URL 저장 (DataStore)
│   └── MessageLog.kt              # 메시지 로그 + 중복 방지 캐시
├── receiver/
│   └── SMSReceiver.kt             # SMS/MMS BroadcastReceiver
├── service/
│   ├── ServiceDaemon.kt           # Foreground Service
│   └── NotificationListenerSvc.kt # RCS 알림 캡처
├── slack/
│   ├── SlackApi.kt                # Retrofit 인터페이스
│   ├── SlackWebHook.kt            # Slack 메시지 빌더
│   └── SlackWebHookWorker.kt      # WorkManager 기반 전송
└── ui/
    ├── theme/                     # Material3 테마
    ├── screen/
    │   └── MainScreen.kt         # 메인 화면 + ViewModel
    └── component/
        ├── StatusIndicator.kt     # 서비스 상태 표시
        ├── MessageLogList.kt      # 메시지 로그 목록
        └── WebhookUrlInput.kt     # Webhook URL 입력
```

## 설정 방법

1. Slack에서 **Incoming Webhooks** 앱을 추가하고 Webhook URL을 발급받습니다.
2. 앱을 설치하고 실행합니다.
3. SMS/MMS 및 알림 관련 권한을 허용합니다.
4. Webhook URL에서 `https://hooks.slack.com/` 뒤의 경로만 입력합니다.
   - 예: `services/T50EEI8EC23/BF2NFNNOI/dcoDWwijylifelj29f923Zf`
5. **저장** 버튼을 누릅니다.
6. **전송 테스트** 버튼으로 Slack 연동을 확인합니다.
7. RCS 메시지도 수신하려면, **설정 > 알림 접근** 에서 RouteSMS를 활성화합니다.

## 필요 권한

| 권한 | 용도 |
|---|---|
| `RECEIVE_SMS` | SMS 수신 감지 |
| `RECEIVE_MMS` | MMS 수신 감지 |
| `READ_SMS` | SMS 내용 읽기 |
| `INTERNET` | Slack Webhook 전송 |
| `POST_NOTIFICATIONS` | Foreground Service 알림 (Android 13+) |
| `FOREGROUND_SERVICE` | 백그라운드 동작 유지 |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | RCS 알림 캡처 |

## 주의사항

이 앱을 설치하면 수신되는 **모든 메시지**가 설정된 Slack 채널로 전달됩니다. 제3자로부터 설치를 권유받았을 경우, 개인정보가 유출될 수 있으니 주의하세요.

## 빌드

```bash
# JDK 17 이상 필요
./gradlew assembleDebug
```

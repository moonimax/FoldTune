# FoldTune

갤럭시 Z Fold 계열의 **힌지 각도**를 읽어서, 각도에 맞는 음을 실시간으로 합성해 내보내는 앱입니다.
접었다 폈다 하면 음이 스케일 위로 스냅되면서 오토튠처럼 미끄러집니다.

## APK 만들기 (셋 중 하나)

안드로이드 앱은 소스를 APK로 컴파일해야 폰에 설치됩니다. 압축만 풀어서는 실행되지 않습니다.

### A. GitHub에 올려서 자동 빌드 — 아무것도 설치 안 함

1. GitHub에 빈 저장소를 만들고 이 폴더 전체를 업로드 (웹 UI의 "uploading an existing file"로 드래그해도 됨)
2. 푸시되는 즉시 **Actions** 탭에서 빌드가 돌아갑니다 (5분 내외)
3. 완료되면 그 실행 화면 아래 **Artifacts → FoldTune-debug-apk** 를 다운로드
4. 압축을 풀면 `app-debug.apk` — 폰으로 옮겨 설치

`.github/workflows/build.yml` 이 이 일을 합니다.

### B. 커맨드라인 (JDK 17만 있으면 됨)

```bash
./gradlew assembleDebug
# 결과: app/build/outputs/apk/debug/app-debug.apk
```

Android SDK가 없으면 `local.properties` 에 `sdk.dir=/경로/Android/sdk` 를 적거나
`ANDROID_HOME` 환경변수를 설정해야 합니다.

폰이 USB로 연결돼 있다면 한 방에:

```bash
./gradlew installDebug
```

### C. Android Studio

**Open** → `FoldTune` 폴더 선택 → 싱크 후 Run.
APK만 필요하면 Build → Build Bundle(s)/APK(s) → Build APK(s).

## 폰에 설치하기

APK를 Fold로 옮긴 뒤(USB, 드라이브, 카톡 나에게 보내기 등) 파일 앱에서 탭하면
"이 출처의 앱 설치 허용"을 묻습니다. 허용하면 설치됩니다.
디버그 서명 APK라 플레이스토어 없이 그냥 깔립니다.

## 실기기 없이 테스트하기

Android Studio의 폴더블 에뮬레이터(Pixel Fold / 7.6" Fold-in)를 띄우고
**Extended controls → Virtual sensors → Hinge angle** 슬라이더를 움직이면 됩니다.

힌지 센서가 아예 없는 기기에서는 화면 아래쪽에 각도 슬라이더가 자동으로 나타나므로
로직만 따로 확인할 수 있습니다.

## 구조

| 파일 | 역할 |
|---|---|
| `MainActivity.kt` | 힌지 센서 등록 + 스무딩, Compose UI |
| `ToneEngine.kt` | AudioTrack 사인파 합성 (글라이드, 배음, 클릭 방지) |
| `Tuning.kt` | 각도 → 스케일 스냅 → MIDI → 주파수 변환 |

## 튜닝 포인트

- **글라이드 슬라이더** — 값이 작을수록 음 사이를 느리게 미끄러집니다. 오토튠 느낌의 핵심.
- `Tuning.MIN_ANGLE` (기본 25°) — 이보다 접으면 음소거. 완전히 접었을 때 소리가 남는 게 싫으면 올리세요.
- `Tuning.OCTAVES` / `ROOT_MIDI` — 음역대 조절.
- `ToneEngine.harmonics` — 0이면 순수 사인파, 0.4쯤이면 오르간에 가까워집니다.
- `MainActivity`의 `smoothed + (raw - smoothed) * 0.25f` — 계수를 낮추면 더 부드럽지만 반응이 느려집니다.

## 주의사항

- `minSdk = 30`: `TYPE_HINGE_ANGLE`이 Android 11부터 들어왔습니다.
- 매니페스트의 `android:configChanges`가 없으면 접을 때마다 액티비티가 재생성돼 소리가 끊깁니다.
- 힌지 센서는 권한이 필요 없습니다.

## 다음으로 해볼 만한 것

- 각속도(각도의 미분)를 비브라토 깊이나 볼륨에 연결
- 바깥 화면 / 안쪽 화면 상태에 따라 음색 전환
- 폴드 각도를 필터 컷오프에 매핑하고 음정은 터치로 따로 제어

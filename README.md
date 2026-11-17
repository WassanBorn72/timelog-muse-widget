# 生活ボタン / Timelog Muse Widget

**アプリは永続的な履歴・ログを保持しません。端末内には送信のための一時的な未送信キューだけを置き、送信成功後に削除します。記録の保存とカレンダーへの書き込みは Muse 側の担当です。**

Android のホーム画面から「就寝・起床・出発・帰宅・今後の予定変更」を記録し、GitHub の private リポジトリに **1イベント = 1 Issue** として送る個人用アプリです。Kotlin / RemoteViews、パッケージ名 `app.timelog.musewidget`。Android 8.0（API 26）以上に対応します。

## ビルドとインストール

Android Studio でこのフォルダを開き、SDK Platform 35、Build Tools 35.0.0、JDK 17 以上を用意してください。Gradle Wrapper（8.13、SHA-256 検証付き）を同梱しています。

```sh
# ANDROID_HOME に SDK の場所を設定、または local.properties に sdk.dir を設定
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android Studio の Run から実機へインストールすることもできます。GitHub Actions でも同じビルドを実行し、debug APK を成果物として保存します。debug APK は動作確認用です。継続利用・配布用の release APK は Android Studio の **Generate Signed Bundle / APK** で本人の署名鍵を使って作成し、鍵を安全に保管してください。同じアプリを更新する場合は同じ署名鍵が必要です。

## コードとイベント送信先を分ける

- **コード・README・Releases 用：`WassanBorn72/timelog-muse-widget`**。public にできますが、生活イベントの Issue は投稿しません。アプリもこの送信先を拒否します。
- **Muse へのイベント送信用：`WassanBorn72/timelog-events`**。必ず **private** にします。新規インストールのデフォルト送信先です。アプリはリポジトリを自動作成しません。
- 他の人が使う場合は、各自の **private リポジトリと自分のトークン** を用意し、アプリ設定で送信先を変更してください。受信側の Muse にもそのリポジトリを読める接続が必要です。

Muse に直接メッセージを送る API / Webhook はありません。GitHub Issue が Muse への受け渡し手段です。端末内には未送信キューだけを保持し、送信成功後に削除します。

### public 化前の切り替え手順

1. GitHub の **New repository** で `timelog-events` を作り、**Private** を選択します。Settings → General → Features で Issues が有効なことを確認します。
2. 以下の手順でイベント用リポジトリだけを対象にしたトークンを発行し、更新した APK をインストールして送信先とトークンを設定します。
3. ボタンを1回押し、private のイベント用リポジトリに Issue が届くこと、未送信件数が減ること、Muse がその Issue を読めることを確認します。
4. コード用リポジトリの **open / closed 両方の既存 Issue、PR、添付ファイル、コミット履歴、Releases** に生活ログや秘密情報がないか確認します。Issue を閉じるだけでは公開を防げません。過去にトークンを掲載した場合は削除に加えて失効が必要です。
5. **その後で**コード用リポジトリの Settings → General → Danger Zone → Change repository visibility から public に変更します。確認完了までは private のままにしてください。古い APK を使う他の端末も停止または更新します。

旧版から更新した場合、保存済み設定のコード用送信先はデフォルトのイベント用送信先として扱います。旧コード用リポジトリを宛先にした未送信キューだけは、送信時に現在の設定先へ切り替えます（ID・時刻・イベント本体は維持）。その他の送信先は変更しません。古いコード用トークンは自動的に権限が変わらないので、private イベント用のトークンを再入力してください。既に投稿済みの Issue は移動しません。

## 初期設定：GitHub トークン

1. GitHub のイベント用リポジトリ `WassanBorn72/timelog-events` が **Private** で、Settings → General → Features → **Issues** が有効になっていることを確認します。アプリは公開/非公開設定を変更しません。
2. GitHub → Settings → Developer settings → Personal access tokens → **Fine-grained tokens** → Generate new token を開きます。
3. Resource owner に対象の所有者を選び、有効期限を設定します。Repository access は **Only select repositories** にして、イベント用 private リポジトリだけを選びます。コード用リポジトリは選びません。
4. Repository permissions の **Issues: Read and write** だけを追加します（Metadata の読み取りは GitHub が必須で付与します）。Contents 等の書き込み権限は不要です。組織リポジトリの場合は必要な管理者承認も済ませます。
5. 発行したトークンをスマホの「生活ボタン」設定画面に入力し、送信先 `WassanBorn72/timelog-events` を確認して「設定を保存・再送」を押します。トークンはチャット、ソースコード、Issue、コミットに貼り付けないでください。
6. ホーム画面の空き領域を長押し → ウィジェット → **生活ボタン** を追加します。大きさはホームアプリに合わせて調整できます。

設定画面でトークンを空欄にした場合は既存値を維持します。削除は「保存済みトークンを削除」を使います。期限切れ・失効時は新しいトークンを入力して再送してください。トークンは Android Keystore の AES-GCM 鍵で暗号化してアプリ専用領域に保存し、バックアップを無効にしています。端末外に保存しません。アンインストールやアプリデータ削除で未送信記録と設定は失われます。

## 記録・送信の動作

- ボタンは画面を開かず BroadcastReceiver で処理し、タップ時刻を採取して SQLite に永続化します。同じ日に何回でも記録できます。保存後に Toast とウィジェット表示で知らせます。
- トークンがなければ「トークン未設定」と表示し、記録は残ります。設定画面には未送信件数と最新状態を表示します。
- WorkManager はネットワーク接続時に保存順で送信します。送信成功（HTTP 201）を確認した記録だけを削除し、それ以外は先頭を保持して指数バックオフで再試行します。後続のイベントは先頭の成功を待ちます。
- 再起動後も WorkManager が処理を引き継ぎます。15分間隔の復旧ジョブも同じ直列送信チェーンに追加するため、同時送信しません。Android の省電力制御で送信が遅れる場合があります。「設定を保存・再送」でも再送を予約できます。強制停止後はアプリを一度開いてください。
- 送信先はイベントごとに記録時の設定を保存します。設定を変えても過去の記録を別リポジトリに転送しません（旧コード用宛先の安全な切り替えだけは上記の例外です）。古い送信先への権限がない場合は、その権限を復旧するまでキューが待機します。
- 「今後の予定変更」は変更があった事実と時刻だけを記録します。予定の詳細入力はありません。

Issue タイトルの例：`sleep_start 2026-11-17T23:42:00+09:00`

本文は次の JSON をコードブロックに入れます。時刻は端末のタイムゾーン付き ISO 8601 形式です。

```json
{
  "id": "2f1958e3-7c3f-48f6-8f88-ec8619ca06de",
  "type": "sleep_start",
  "timestamp": "2026-11-17T23:42:00+09:00",
  "device": "Pixel 9"
}
```

イベント種別：`sleep_start` / `sleep_end` / `departure` / `arrival` / `schedule_change`。
Muse は本文を読み、処理後に `processed` ラベルを付けるか Issue をクローズします。アプリはラベルやクローズ状態を操作しません。

**重複について：** GitHub が Issue 作成を受理した直後に通信が切れたり、成功後のローカル削除前にプロセスが終了した場合は、再送で重複 Issue が作られる可能性があります。GitHub の Issue 作成 API は冪等キーを提供しないため、本アプリは記録消失を避ける at-least-once 方式です。Muse は `id` が同じイベントを一度だけ処理してください。

## 検証

ローカル JVM テストは、キューの送信順、失敗時の保持、復帰時の再送、停止時の保持、送信先入力の制約を検証します。端末テストは SQLite の再オープン後の保持・順序、暗号化トークンの保存/削除、全イベントの JSON を検証します。

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
# 実機またはエミュレータを接続して実行（アプリのテスト用データが削除されます）
./gradlew connectedDebugAndroidTest
```

実機での受け入れ確認：

1. トークンなしで5種類を押す。画面が開かず、未送信件数が5になり「トークン未設定」が表示されること。
2. トークンを保存し、5件が順に Issue になり、本文の種別・時刻・IDを確認すること。
3. 機内モードで連続タップし、未送信件数が増えること。プロセスを終了・再起動しても残ること。
4. 機内モードを解除し、OS の実行許可後に記録が送信されること。省電力制御で遅れる場合は設定画面から再送すること。
5. 無効なトークンで記録し、HTTP エラー表示と記録の保持を確認すること。有効なトークンに戻して送信されること。
6. ホームアプリからウィジェットを複数追加しても全ボタンが動き、タップでアプリ画面が開かないこと。

実機確認では個人の生活記録と区別できるテスト用 private リポジトリの使用を推奨します。

## CI と自分用の署名済み APK

Actions の `Android` ワークフローは push / PR / 手動実行に対応します。Ubuntu 24.04、JDK 17、Command-line Tools 13114758、SDK 35 / Build Tools 35.0.0 を使います。廃止された SDK の `tools` パッケージは要求しません。`build` は APK ビルド後に JVM テストと Lint、`device-tests` は API 29 のエミュレータで端末テストを行います。失敗時にも取得できたテスト・Lint レポートを成果物として残します。debug APK は署名済み release APK の代わりにはなりません。

署名済み APK は Muse 側のローカル環境で作成してください。

1. CI の成功したコミットをローカルで取得します。Android Studio に JDK 17 以上と SDK 35 / Build Tools 35.0.0 を用意します。
2. Android Studio の **Build → Generate Signed Bundle / APK → APK** を開きます。
3. 既存の鍵があれば同じ鍵を選びます。初回のみ、**リポジトリの外**に keystore を作成し、alias・パスワードとともに安全に保管・バックアップします。鍵やパスワードをチャット・GitHub・CI 成果物に置かないでください。
4. `release` を選択し、署名付き APK を生成します。本プロジェクトは署名情報をソースや Gradle ファイルに保存しません。パスワードを保存する場合も Android Studio の安全な保存機能を使います。
5. Android SDK の `apksigner verify --verbose --print-certs <APKのパス>` で署名を検証してから端末へインストールします。
6. debug 版と release 版で署名が異なる場合は上書きできません。未送信件数を0にしてから debug 版をアンインストールし、release 版を入れてトークンを再設定してください（アンインストールで設定と未送信キューが消えます）。
7. 継続的に更新する場合は同じ署名鍵を使用し、更新ごとに `versionCode` と `versionName` を上げてください。

署名鍵、alias、鍵のパスワードがローカルに揃うまでは release APK の署名は完了できません。CI のビルド成功だけでは、実機での送信成功や署名済み APK の完成を意味しません。

未署名 release APK は専用の **Unsigned release APK** Actions ワークフローで生成できます。成功した run の artifact `timelog-release-unsigned` を取得し、Muse 側でローカル署名します。既存の debug ワークフローは変更しません。詳しくは [APK の取得・署名・Release 配布手順](docs/APK_RELEASE.md) を参照してください。

# 未署名 APK の取得・ローカル署名・配布

## CI から取得

GitHub Actions → **Unsigned release APK** → 成功した run → Artifacts → **timelog-release-unsigned** をダウンロードして展開します。GitHub にログインしてダウンロードしてください。公開リポジトリでも artifact 取得には認証が必要な場合があります。artifact は30日で期限切れになるため、Muse 側のローカルにも保存します。

このワークフローは `assembleRelease` のみを行い、署名鍵・パスワード・署名用 Secret を使いません。署名も GitHub Release 公開も自動では行いません。既存の `Android` ワークフローによるテスト結果も確認してください。

- `timelog-release-unsigned.apk`：署名前の APK。そのままではインストールできません。
- `SHA256SUMS`：未署名 APK の SHA-256。
- `BUILD-INFO.txt`：ソースコミット、ref、run URL。
- `APK-METADATA.txt`：パッケージ・バージョン・対応 API の情報。

取得元 run のコミットが、配布する main / リリースタグに一致することを確認し、展開先で `sha256sum -c SHA256SUMS` を実行します。検証済みの feature ブランチ APK を使う場合も、マージ後の main とソース内容が同じことを確認してください。main へのマージ後は Actions で手動実行できます。

## Muse 側でローカル署名

Android SDK Build Tools 35.0.0 の `zipalign` と `apksigner` を使用します。署名鍵はリポジトリ外に保存し、安全なバックアップを取ります。鍵やパスワードをチャット、GitHub、CI、配布 ZIP に含めないでください。パスワードはコマンド実行時の対話入力にし、コマンド引数やシェル履歴に残さないでください。

```sh
# CI ですでに alignment を確認しています。署名後に zipalign で書き換えないこと。
zipalign -c -P 16 4 timelog-release-unsigned.apk
apksigner sign --ks /local/path/timelog-release.jks --ks-key-alias YOUR_ALIAS \
  --out timelog-1.1-release.apk timelog-release-unsigned.apk
apksigner verify --verbose --print-certs timelog-1.1-release.apk
sha256sum timelog-1.1-release.apk > timelog-1.1-release.apk.sha256
adb install -r timelog-1.1-release.apk
```

初回の鍵作成には Android Studio の **Generate Signed Bundle / APK → APK → Create new** を利用できます。継続更新には同じ鍵が必要です。既存の release 版がある場合は必ずその署名鍵を使います。

debug 版とは署名が異なるため、release 版で上書きできないことがあります。未送信件数を0にしてから debug 版をアンインストールしてください。アンインストールでトークンと未送信キューが削除されるため、release 版で private イベント送信先とトークンを再設定します。

## GitHub Release で配布

1. 現在は `versionCode=2` / `versionName=1.1` です。更新時はビルド前に versionCode を増やし、versionName も更新します。
2. main の配布対象コミットに `v1.1.0` 等のタグを付け、GitHub Releases でそのタグの **Draft release** を作ります。
3. アプリの対応 Android バージョン、初期設定、private の `timelog-events` を使うこと、署名証明書の SHA-256 fingerprint を説明に記載します。
4. **署名済み APK と署名後の SHA-256 ファイルだけ**を Release assets に添付します。未署名 APK、keystore、パスワード、PAT、端末のデータベースは添付しません。
5. 実機でインストールと private リポジトリへの送信を確認してから公開します。

public の GitHub Release に置いた APK は誰でもダウンロードできます。APK に個人用トークンは含まれませんが、自分用のみなら Release を公開せず、署名済み APK をローカル保管して直接インストールする方法でも十分です。各利用者のトークン・イベント送信先は端末で設定します。

artifact が認証制限で Muse から取れない場合は、ユーザー本人がログインしてダウンロードし、**鍵を含まない未署名 APK の ZIP**だけを Muse 側へ渡してください。APK を Git のブランチにコミットする必要はありません。

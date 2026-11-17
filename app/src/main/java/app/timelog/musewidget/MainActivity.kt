package app.timelog.musewidget

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var summary: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val update = object : Runnable {
        override fun run() {
            summary.text = "未送信 ${Store(this@MainActivity).use { it.count() }} 件\n${Settings(this@MainActivity).status}"
            handler.postDelayed(this, 2000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        val settings = Settings(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        fun text(value: String) { layout.addView(TextView(this).apply { text = value; textSize = 18f }) }
        text("生活ボタン 設定")
        summary = TextView(this); layout.addView(summary)
        text("送信先 private リポジトリ（owner/repository）")
        val repository = EditText(this).apply { setSingleLine(); setText(settings.repository); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
        layout.addView(repository)
        text("GitHub fine-grained token（空欄は既存値を維持）")
        val token = EditText(this).apply {
            setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            isSaveEnabled = false
        }
        layout.addView(token)
        layout.addView(Button(this).apply {
            text = "設定を保存・再送"
            setOnClickListener {
                val repo = repository.text.toString().trim()
                val newToken = token.text.toString().trim()
                if (!Settings.validRepository(repo)) { repository.error = "private の owner/repository を入力してください。コード用リポジトリは指定できません"; return@setOnClickListener }
                if (newToken.any { it.isWhitespace() || it.code < 32 }) { token.error = "トークンの形式を確認してください"; return@setOnClickListener }
                try {
                    if (newToken.isNotEmpty()) settings.saveToken(newToken)
                    settings.repository = repo
                    settings.status = "設定を保存しました"
                    token.text.clear()
                    SyncWorker.ensurePeriodic(this@MainActivity)
                    SyncWorker.enqueue(this@MainActivity)
                    LifeWidget.refresh(this@MainActivity)
                    Toast.makeText(this@MainActivity, "保存しました", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { Toast.makeText(this@MainActivity, "設定を保存できませんでした", Toast.LENGTH_LONG).show() }
            }
        })
        layout.addView(Button(this).apply {
            text = "保存済みトークンを削除"
            setOnClickListener {
                settings.saveToken(""); settings.status = "トークン未設定・記録は保持中"
                LifeWidget.refresh(this@MainActivity)
            }
        })
        text("ホーム画面を長押し → ウィジェット → 生活ボタンを追加してください。送信先の変更は新しい記録から適用されます。")
        setContentView(ScrollView(this).apply {
            addView(layout, ViewGroup.LayoutParams(-1, -2))
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
        })
        SyncWorker.ensurePeriodic(this)
        SyncWorker.enqueue(this)
    }
    override fun onResume() { super.onResume(); handler.post(update) }
    override fun onPause() { handler.removeCallbacks(update); super.onPause() }
}

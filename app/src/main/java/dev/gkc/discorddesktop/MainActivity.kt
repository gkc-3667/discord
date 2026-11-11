package dev.gkc.discorddesktop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private const val REDIRECT_URI = "discorddesktop://oauth/callback"
private const val PREFS = "discord_desktop_layout"

private data class DiscordUser(val id: String, val username: String, val globalName: String?)
private data class DiscordGuild(val id: String, val name: String, val owner: Boolean)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var clientId by mutableStateOf("")
    private var user by mutableStateOf<DiscordUser?>(null)
    private var guilds by mutableStateOf<List<DiscordGuild>>(emptyList())
    private var selectedGuild by mutableStateOf<DiscordGuild?>(null)
    private var status by mutableStateOf("Chưa kết nối Discord")
    private var busy by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = prefs.getString("client_id", "") ?: ""
        setContent { DesktopApp() }
        intent?.data?.let { handleOAuthCallback(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { handleOAuthCallback(it) }
    }

    @Composable
    private fun DesktopApp() {
        MaterialTheme(colorScheme = darkColorScheme(
            background = Color(0xFF1E1F22),
            surface = Color(0xFF2B2D31),
            primary = Color(0xFF5865F2),
            secondary = Color(0xFFB5BAC1)
        )) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            ServerRail()
                            GuildPane(Modifier.width(250.dp).fillMaxHeight())
                            MainPane(Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f)) {
                                ServerRail()
                                MainPane(Modifier.weight(1f).fillMaxHeight())
                            }
                            GuildPane(Modifier.fillMaxWidth().heightIn(max = 230.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ServerRail() {
        Column(
            Modifier.width(68.dp).fillMaxHeight().background(Color(0xFF1A1B1E)).padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(44.dp).background(Color(0xFF5865F2), RoundedCornerShape(15.dp)), Alignment.Center) {
                Text("D", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(Modifier.width(32.dp), color = Color(0xFF3F4147))
            guilds.take(18).forEach { guild ->
                Box(
                    Modifier.size(42.dp).background(
                        if (selectedGuild?.id == guild.id) Color(0xFF5865F2) else Color(0xFF35373C),
                        RoundedCornerShape(15.dp)
                    ).clickable { selectedGuild = guild },
                    Alignment.Center
                ) {
                    Text(guild.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            if (guilds.isEmpty()) {
                Text("+", color = Color(0xFF3BA55D), fontSize = 26.sp)
            }
        }
    }

    @Composable
    private fun GuildPane(modifier: Modifier = Modifier) {
        Column(modifier.background(Color(0xFF2B2D31))) {
            Text("Discord", Modifier.fillMaxWidth().padding(18.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            HorizontalDivider(color = Color(0xFF202225))
            Text("SERVERS", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = Color(0xFF949BA4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (guilds.isEmpty()) {
                Text("Đăng nhập để tải danh sách server.", Modifier.padding(16.dp), color = Color(0xFFB5BAC1), fontSize = 13.sp)
            } else {
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(guilds) { guild ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedGuild = guild }
                                .background(if (selectedGuild?.id == guild.id) Color(0xFF404249) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(guild.name, color = Color(0xFFDBDEE1), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF202225))
            Text(status, Modifier.padding(12.dp), color = Color(0xFF949BA4), fontSize = 11.sp)
        }
    }

    @Composable
    private fun MainPane(modifier: Modifier = Modifier) {
        Column(modifier.background(Color(0xFF313338)).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DISCORD DESKTOP LAYOUT", color = Color(0xFF949BA4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(selectedGuild?.name ?: "Trang chủ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                if (user != null) {
                    Text("●", color = Color(0xFF3BA55D), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (user == null) {
                Text("Kết nối tài khoản Discord", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Nhập Application Client ID của ứng dụng OAuth2 Discord. Không nhập mật khẩu, token hay Client Secret.", color = Color(0xFFB5BAC1), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it.trim(); prefs.edit().putString("client_id", clientId).apply() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Discord Application Client ID") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { beginOAuth() }, enabled = !busy && clientId.isNotBlank()) {
                    Text(if (busy) "Đang xử lý..." else "Đăng nhập Discord")
                }
                Spacer(Modifier.height(10.dp))
                Text(status, color = Color(0xFFFFC857), fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                InfoCard()
            } else {
                Text("Xin chào, ${user?.globalName ?: user?.username}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("@${user?.username}", color = Color(0xFFB5BAC1), fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                InfoCard()
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = { signOut() }) { Text("Đăng xuất") }
            }
        }
    }

    @Composable
    private fun InfoCard() {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF232428)), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Bản thử nghiệm giao diện PC", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Hiện tại ứng dụng đăng nhập OAuth2 và hiển thị tài khoản + danh sách server. Discord không cấp quyền đọc/gửi tin nhắn tổng quát cho ứng dụng OAuth người dùng này. Vì vậy chat, kênh, voice và DM chưa có; đây chưa phải ứng dụng thay thế Discord chính thức.",
                    color = Color(0xFFB5BAC1), fontSize = 13.sp, lineHeight = 19.sp
                )
            }
        }
    }

    private fun beginOAuth() {
        if (clientId.isBlank()) return
        busy = true
        status = "Đang mở đăng nhập Discord..."
        val verifierBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
        val challengeBytes = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)
        val state = ByteArray(24).also { SecureRandom().nextBytes(it) }.let {
            Base64.getUrlEncoder().withoutPadding().encodeToString(it)
        }
        prefs.edit().putString("pkce_verifier", verifier).putString("oauth_state", state).apply()
        val url = "https://discord.com/oauth2/authorize" +
            "?client_id=${enc(clientId)}" +
            "&response_type=code" +
            "&redirect_uri=${enc(REDIRECT_URI)}" +
            "&scope=${enc("identify guilds")}" +
            "&state=${enc(state)}" +
            "&code_challenge=${enc(challenge)}" +
            "&code_challenge_method=S256"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            status = "Hoàn tất đăng nhập trong trình duyệt rồi quay lại ứng dụng."
        } catch (e: Exception) {
            status = "Không mở được trình duyệt: ${e.message}"
            busy = false
        }
    }

    private fun handleOAuthCallback(uri: Uri) {
        if (uri.scheme != "discorddesktop" || uri.host != "oauth") return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            status = "Discord từ chối đăng nhập: $error"
            busy = false
            return
        }
        val state = uri.getQueryParameter("state")
        val expectedState = prefs.getString("oauth_state", null)
        val code = uri.getQueryParameter("code")
        if (state == null || state != expectedState || code == null) {
            status = "Callback OAuth không hợp lệ. Hãy thử đăng nhập lại."
            busy = false
            return
        }
        val verifier = prefs.getString("pkce_verifier", null) ?: run {
            status = "Thiếu PKCE verifier. Hãy đăng nhập lại."
            busy = false
            return
        }
        busy = true
        status = "Đang kết nối Discord..."
        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { exchangeCode(code, verifier) }
                prefs.edit().putString("access_token", token).remove("pkce_verifier").remove("oauth_state").apply()
                val result = withContext(Dispatchers.IO) { loadDiscordData(token) }
                user = result.first
                guilds = result.second
                selectedGuild = guilds.firstOrNull()
                status = "Đã kết nối • ${guilds.size} server"
            } catch (e: Exception) {
                status = "Lỗi kết nối: ${e.message ?: "không xác định"}"
            } finally {
                busy = false
            }
        }
    }

    private fun exchangeCode(code: String, verifier: String): String {
        val params = listOf(
            "client_id" to clientId,
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to verifier
        )
        val conn = URL("https://discord.com/api/v10/oauth2/token").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(params.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }.toByteArray()) }
        val body = readResponse(conn)
        val httpStatus = conn.responseCode
        conn.disconnect()
        val json = JSONObject(body)
        if (httpStatus !in 200..299) error(json.optString("error_description", json.optString("error", "OAuth failed")))
        return json.getString("access_token")
    }

    private fun loadDiscordData(token: String): Pair<DiscordUser, List<DiscordGuild>> {
        val me = JSONObject(apiGet("https://discord.com/api/v10/users/@me", token))
        val user = DiscordUser(me.getString("id"), me.getString("username"), me.optString("global_name").takeIf { it.isNotBlank() && it != "null" })
        val arr = JSONArray(apiGet("https://discord.com/api/v10/users/@me/guilds", token))
        val guilds = (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            DiscordGuild(g.getString("id"), g.getString("name"), g.optBoolean("owner"))
        }
        return user to guilds
    }

    private fun apiGet(url: String, token: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("User-Agent", "DiscordDesktopLayout/0.1.0")
        val body = readResponse(conn)
        val httpStatus = conn.responseCode
        conn.disconnect()
        if (httpStatus !in 200..299) error("Discord API HTTP $httpStatus: $body")
        return body
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return body
    }

    private fun signOut() {
        prefs.edit().remove("access_token").apply()
        user = null
        guilds = emptyList()
        selectedGuild = null
        status = "Đã đăng xuất"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

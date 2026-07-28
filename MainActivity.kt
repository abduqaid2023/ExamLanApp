package com.examlan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.examlan.app.ui.StudentScreen
import com.examlan.app.ui.TeacherScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "role_select") {
        composable("role_select") { RoleSelectionScreen(navController) }
        composable("teacher") { TeacherScreen() }
        composable("student") { StudentScreen() }
    }
}

@Composable
fun RoleSelectionScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("اختر وضع التشغيل", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navController.navigate("teacher") }, modifier = Modifier.fillMaxWidth()) {
            Text("وضع الأستاذ (إنشاء اختبار)")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate("student") }, modifier = Modifier.fillMaxWidth()) {
            Text("وضع الطالب (الانضمام لاختبار)")
        }
    }
}

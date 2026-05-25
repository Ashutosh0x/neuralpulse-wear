package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphaAdminNavigationDrawer(
    drawerState: DrawerState,
    onExportFhir: () -> Unit = {},
    onComplianceCheck: () -> Unit = {},
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AlphaBlack,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. PARTNER SECURITY IDENTIFICATION HEADER
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "PARTNER SECURITY ATTESTATION",
                            fontSize = 11.sp,
                            color = AlphaAccentBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SHA-256 Verified Node",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "App ID: com.neuralpulse.app",
                            fontSize = 12.sp,
                            color = AlphaTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1F1F22), thickness = 1.dp)

                    // 2. ADMIN UTILITIES SECTIONS
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp), 
                        modifier = Modifier.weight(1f)
                    ) {
                        DrawerCategoryLabel(text = "DATA PIPELINES")
                        DrawerActionRow(
                            icon = Icons.Outlined.HistoryEdu, 
                            title = "Export HL7 FHIR Logs", 
                            onClick = onExportFhir
                        )
                        DrawerActionRow(
                            icon = Icons.Outlined.CloudSync, 
                            title = "Force Store Sync"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DrawerCategoryLabel(text = "PRIVACY SANDBOX BOUNDARIES")
                        DrawerActionRow(
                            icon = Icons.Outlined.Shield, 
                            title = "Gemma SLM Constraints"
                        )
                        DrawerActionRow(
                            icon = Icons.Outlined.Memory, 
                            title = "NPU Memory Allocation"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DrawerCategoryLabel(text = "REGULATORY LEDGER")
                        DrawerActionRow(
                            icon = Icons.Outlined.Gavel, 
                            title = "FDA Wellness Compliance", 
                            onClick = onComplianceCheck
                        )
                    }

                    // 3. ECOSYSTEM VERSION LOCK FOOTER
                    Text(
                        text = "AlphaHealth Ecosystem v1.1.0-Release\nBuild Target API 35 | Java 17 Engine",
                        fontSize = 10.sp,
                        color = AlphaTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun DrawerCategoryLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = AlphaTextSecondary,
        letterSpacing = 1.sp
    )
}

@Composable
fun DrawerActionRow(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = title, 
                tint = Color.White, 
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.floently.learn.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KVSpacing {
    val xs = 4.dp
    val s = 8.dp
    val sm = 12.dp
    val m = 16.dp
    val ml = 20.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
    val huge = 64.dp
}

object KVRadius {
    val s = 10.dp
    val m = 14.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object KVColor {
    val Canvas = Color(0xFF07060A)
    val CanvasRaised = Color(0xFF0B0810)
    val Surface1 = Color(0xFF0F0C14)
    val Surface2 = Color(0xFF15101D)
    val Surface3 = Color(0xFF1B1427)
    val Brand = Color(0xFF8B5CFF)
    val BrandBright = Color(0xFFB998FF)
    val BrandDeep = Color(0xFF5A34CC)
    val BrandTint = Color(0xFF24183A)
    val TextPrimary = Color(0xFFF7F3FF)
    val TextSecondary = Color(0xFFB9B1C4)
    val TextTertiary = Color(0xFF81788D)
    val BorderSoft = Color(0xFF241E2C)
}

enum class KVCardStyle {
    Hero,
    Standard,
    Compact
}

private fun KVCardStyle.radius() = when (this) {
    KVCardStyle.Hero -> KVRadius.xxl
    KVCardStyle.Standard, KVCardStyle.Compact -> KVRadius.xl
}

private fun KVCardStyle.padding() = when (this) {
    KVCardStyle.Hero -> KVSpacing.l
    KVCardStyle.Standard -> KVSpacing.ml
    KVCardStyle.Compact -> KVSpacing.m
}

private fun KVCardStyle.minimumHeight() = when (this) {
    KVCardStyle.Hero -> 184.dp
    KVCardStyle.Standard -> 132.dp
    KVCardStyle.Compact -> 84.dp
}

private fun KVCardStyle.fill() = when (this) {
    KVCardStyle.Hero -> KVColor.BrandTint
    KVCardStyle.Standard -> KVColor.Surface2
    KVCardStyle.Compact -> KVColor.Surface1
}

@Composable
fun KieliValmisCardSurface(
    modifier: Modifier = Modifier,
    style: KVCardStyle = KVCardStyle.Standard,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = style.fill(),
        shape = RoundedCornerShape(style.radius()),
        border = BorderStroke(1.dp, KVColor.BorderSoft),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = style.minimumHeight())
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.sm),
            modifier = Modifier.padding(style.padding()),
            content = content
        )
    }
}

@Composable
fun KieliValmisIconTile(
    icon: ImageVector,
    contentDescription: String? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(KVColor.Surface3, RoundedCornerShape(KVRadius.m))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = KVColor.BrandBright,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun KieliValmisPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = KVColor.Brand,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(KVRadius.l),
        contentPadding = PaddingValues(horizontal = KVSpacing.ml),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun KieliValmisSectionHeader(title: String) {
    Text(
        text = title,
        color = KVColor.TextPrimary,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun KieliValmisPathwayCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KieliValmisCardSurface(
        style = KVCardStyle.Standard,
        modifier = modifier
            .defaultMinSize(minHeight = 160.dp)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        KieliValmisIconTile(icon)
        Spacer(Modifier.height(KVSpacing.s))
        Text(
            text = title,
            color = KVColor.TextPrimary,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = KVColor.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun KieliValmisSkillCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KieliValmisCardSurface(
        style = KVCardStyle.Standard,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick)
    ) {
        KieliValmisIconTile(icon)
        Text(
            text = title,
            color = KVColor.TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = KVColor.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun KieliValmisReviewCard(
    icon: ImageVector,
    onClick: () -> Unit
) {
    KieliValmisCardSurface(
        style = KVCardStyle.Compact,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KVSpacing.sm)
        ) {
            KieliValmisIconTile(icon)
            Column(
                verticalArrangement = Arrangement.spacedBy(KVSpacing.xs),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Review",
                    color = KVColor.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Your due reviews will appear here after your learning history is connected.",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

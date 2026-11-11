package com.kichikan.negah

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun ringPreview(bytes: ByteArray?, limit: Int) = bytes?.let {
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    BitmapFactory.decodeByteArray(it,0,it.size,bounds)
    if(bounds.outWidth<=0||bounds.outHeight<=0) return@let null
    var sample=1
    while(bounds.outWidth/sample>limit||bounds.outHeight/sample>limit) sample*=2
    BitmapFactory.decodeByteArray(it,0,it.size,BitmapFactory.Options().apply{inSampleSize=sample})?.asImageBitmap()
}

@Composable
fun RingProfileScreen(viewModel: RingProfileViewModel,onBack:()->Unit,modifier:Modifier=Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null){val mime=context.contentResolver.getType(uri);val input=context.contentResolver.openInputStream(uri)
            if(mime!=null&&input!=null)viewModel.saveRingImage(input,mime) else input?.close()}
    }
    val bannerPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null){val mime=context.contentResolver.getType(uri);val input=context.contentResolver.openInputStream(uri)
            if(mime!=null&&input!=null)viewModel.saveRingBanner(input,mime) else input?.close()}
    }
    val image=remember(state.ringImageBytes){ringPreview(state.ringImageBytes,512)}
    val banner=remember(state.bannerBytes){ringPreview(state.bannerBytes,1200)}
    Column(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick=onBack){Text("بازگشت")}
        Text("پروفایل حلقه",style=MaterialTheme.typography.headlineSmall)
        Text("نام، شناسه و تصاویر نمایشی حلقه را تنظیم کنید. اطلاعات این بخش فقط روی همین دستگاه ذخیره می‌شود.",
            style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(state.isLoading) CircularProgressIndicator() else {
            OutlinedTextField(state.nameDraft,viewModel::onNameChanged,label={Text("نام حلقه")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(state.usernameDraft,viewModel::onUsernameChanged,label={Text("شناسهٔ حلقه (اختیاری)")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("تصویر پروفایل حلقه",style=MaterialTheme.typography.titleMedium)
            image?.let{Image(it,"تصویر پروفایل حلقه",Modifier.size(88.dp).clip(CircleShape),contentScale=ContentScale.Crop)}
            TextButton(onClick={imagePicker.launch("image/*")}){Text("انتخاب تصویر حلقه")}
            if(image!=null)TextButton(onClick=viewModel::removeRingImage){Text("حذف تصویر حلقه")}
            Text("بنر حلقه",style=MaterialTheme.typography.titleMedium)
            banner?.let{Image(it,"بنر حلقه",Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)}
            TextButton(onClick={bannerPicker.launch("image/*")}){Text("انتخاب بنر حلقه")}
            if(banner!=null)TextButton(onClick=viewModel::removeRingBanner){Text("حذف بنر حلقه")}
            state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            state.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
            state.mediaMessage?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
            Button(onClick=viewModel::save,enabled=state.hasChanges&&!state.isSaving,modifier=Modifier.fillMaxWidth()){
                Text(if(state.isSaving)"در حال ذخیره..." else "ذخیره")
            }
        }
    }
}

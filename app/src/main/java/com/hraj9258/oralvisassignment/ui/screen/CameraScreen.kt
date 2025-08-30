package com.hraj9258.oralvisassignment.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hraj9258.oralvisassignment.CameraPreview
import com.hraj9258.oralvisassignment.data.SessionState
import com.hraj9258.oralvisassignment.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionViewModel = remember { SessionViewModel(context) }
    
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val capturedImages by sessionViewModel.capturedImages.collectAsStateWithLifecycle()
    val isLoading by sessionViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by sessionViewModel.errorMessage.collectAsStateWithLifecycle()
    
    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: true
        
        if (!cameraGranted) {
            // Handle camera permission denied
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (android.os.Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    // Show error message
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            sessionViewModel.clearError()
        }
        
        Snackbar(
            action = {
                TextButton(onClick = { sessionViewModel.clearError() }) {
                    Text("Dismiss")
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(message)
        }
    }
    
    when (val currentState = sessionState) {
        is SessionState.Idle -> {
            StartSessionView(
                onStartSession = { sessionViewModel.startSession() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        is SessionState.Active -> {
            ActiveSessionView(
                sessionId = currentState.sessionId,
                imageCount = currentState.imageCount,
                capturedImages = capturedImages,
                isLoading = isLoading,
                onCaptureImage = { bitmap -> sessionViewModel.captureImage(bitmap) },
                onEndSession = { name, age -> 
                    sessionViewModel.endSession(name, age)
                },
                onNavigateBack = { 
                    sessionViewModel.resetToIdle()
                    navController.popBackStack() 
                }
            )
        }
        
        is SessionState.Completed -> {
            LaunchedEffect(currentState.sessionId) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
fun StartSessionView(
    onStartSession: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ready to Start Session",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Tap the button below to begin capturing images",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Session", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }
    }
}

@Composable
fun ActiveSessionView(
    sessionId: String,
    imageCount: Int,
    capturedImages: List<Bitmap>,
    isLoading: Boolean,
    onCaptureImage: (Bitmap) -> Unit,
    onEndSession: (String, Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    
    var showEndSessionDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Bar with session info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Session Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Images: $imageCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
        
        // Camera Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(32.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch Camera
            IconButton(
                onClick = {
                    controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Capture Button
            IconButton(
                onClick = {
                    if (!isLoading) {
                        takePhoto(controller, context, onCaptureImage)
                    }
                },
                modifier = Modifier.size(72.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Take Photo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // End Session
            IconButton(
                onClick = { showEndSessionDialog = true },
                enabled = imageCount > 0
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "End Session",
                    tint = if (imageCount > 0) Color.White else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
    
    // End Session Dialog
    if (showEndSessionDialog) {
        EndSessionDialog(
            onDismiss = { showEndSessionDialog = false },
            onConfirm = { name, age ->
                showEndSessionDialog = false
                onEndSession(name, age)
            }
        )
    }
}

@Composable
fun EndSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Session") },
        text = {
            Column {
                Text("Please enter session details:")
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ageInt = age.toIntOrNull()
                    if (name.isNotBlank() && ageInt != null && ageInt > 0) {
                        onConfirm(name, ageInt)
                    }
                },
                enabled = name.isNotBlank() && age.toIntOrNull()?.let { it > 0 } == true
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun takePhoto(
    controller: LifecycleCameraController,
    context: android.content.Context,
    onPhotoTaken: (Bitmap) -> Unit
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                
                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                    // Mirror for front camera
                    if (controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        postScale(-1f, 1f)
                    }
                }
                
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )
                
                onPhotoTaken(rotatedBitmap)
                image.close()
            }
            
            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}
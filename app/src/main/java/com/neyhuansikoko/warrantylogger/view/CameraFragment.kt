package com.neyhuansikoko.warrantylogger.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.databinding.FragmentCameraBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val FLASH_MODE_DEFAULT = ImageCapture.FLASH_MODE_OFF

class CameraFragment : Fragment() {

    private var sharedPreferences: SharedPreferences? = null

    private var _binding: FragmentCameraBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var flashMode: Int = FLASH_MODE_DEFAULT
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var cameraSelector: CameraSelector

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }

        if (granted) {
            startCamera()
        } else {
            Toast.makeText(requireActivity(),
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        flashMode = sharedPreferences?.getInt(FLASH_MODE_KEY, FLASH_MODE_DEFAULT) ?: FLASH_MODE_DEFAULT

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestMultiplePermissions.launch(REQUIRED_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        binding.ivBtnCameraClick.setOnClickListener {
            binding.ivBtnCameraClick.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.anim_rotate))
            takePhoto()
        }
        binding.ivBtnCameraFlash.apply {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    this.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_off, null))
                }
                ImageCapture.FLASH_MODE_ON -> {
                    this.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_on, null))
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    this.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_auto, null))
                }
            }
            this.setOnClickListener {
                when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> {
                        flashMode = ImageCapture.FLASH_MODE_ON
                        binding.ivBtnCameraFlash.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_on, null))
                    }
                    ImageCapture.FLASH_MODE_ON -> {
                        flashMode = ImageCapture.FLASH_MODE_AUTO
                        binding.ivBtnCameraFlash.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_auto, null))
                    }
                    ImageCapture.FLASH_MODE_AUTO -> {
                        flashMode = ImageCapture.FLASH_MODE_OFF
                        binding.ivBtnCameraFlash.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_flash_off, null))
                    }
                }
                sharedPreferences?.edit()?.let {
                    log("Test")
                    it.putInt(FLASH_MODE_KEY, flashMode)
                    it.apply()
                }
                bindCameraUseCases()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                camera!!.cameraControl.setZoomRatio(scale)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)

        binding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }

        sharedViewModel.onResetImageCount()
        sharedViewModel.cameraImageCount.observe(viewLifecycleOwner) { imageCount ->
            if (imageCount > 0) {
                binding.tvCameraImagesCount.text =
                    "$imageCount ${if (imageCount == 1) "image" else "images"} taken"
            } else {
                binding.tvCameraImagesCount.text = getString(R.string.empty)
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val file = File(requireActivity().applicationContext.cacheDir, getUniqueName() + TEMP_IMAGE_SUFFIX)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireActivity()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val outputUri = output.savedUri
//                    outputUri?.toFile()?.let {
//                        val msg = "Photo capture succeeded: ${it.name}"
//                        Toast.makeText(requireActivity().baseContext, msg, Toast.LENGTH_SHORT).show()
//
//                        lifecycleScope.launch(Dispatchers.Default) {
////                            sharedViewModel.onImgCopiedToTemp(it)
//                            withContext(Dispatchers.Main) {
//                                navigateToAddWarranty()
//                            }
//                        }
//                    }
                    outputUri?.let { sharedViewModel.onImagesTaken(it) }
                }
            }
        )

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
            
            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindCameraUseCases() {
        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture)

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

//    private fun navigateToAddWarranty() {
//        val action = CameraFragmentDirections.actionCameraFragmentToAddWarrantyFragment(
//            title = if (sharedViewModel.inputModel.isValid()) {
//                getString(R.string.edit_warranty_title_text)
//            } else {
//                getString(R.string.add_warranty_title_text)
//            }
//        )
//        findNavController().navigate(action)
//    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "WarrantyLogger"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.adapter.ImageListAdapter
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.database.model.isValid
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private val inputWarrantyName: String get() = binding.tilEtAddWarrantyName.text.toString()
    private val inputNote: String get() = binding.tilEtAddNote.text.toString()
    private val inputDuration: String get() = binding.tilEtAddDuration.text.toString()
    private val inputUnit: String get() = binding.actvAddUnit.text.toString()
    private val inputExpirationDate: String get() = binding.tilEtAddExpirationDate.text.toString()
    private val tgCheckedId: Int get() = binding.tgAddDeadline.checkedButtonId

    private lateinit var rvAdapter: ImageListAdapter

    private val imageChooserActivity = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { imageUriList ->
        //Return content URI
        if (!imageUriList.isNullOrEmpty()) {
            sharedViewModel.onImagesRetrieved(imageUriList)
        }
    }
    private val model get() = sharedViewModel.inputModel

    private var onSaveClick: () -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddWarrantyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindModel()
    }

    private fun bindModel() {
        binding.apply {
            btnAddCancel.setOnClickListener {
                findNavController().navigateUp()
            }

            tilEtAddWarrantyName.setText(model.warrantyName)
            tilEtAddNote.setText(model.note)
            tilEtAddPurchaseDate.setText(formatDateMillis(model.purchaseDate))

            val actvAdapter = ArrayAdapter(
                requireContext(),
                R.layout.list_item_unit,
                resources.getStringArray(R.array.time_unit_array_larger)
            )
            actvAddUnit.setAdapter(actvAdapter)
            actvAddUnit.setText(actvAdapter.getItem(0), false)

            rvAdapter = ImageListAdapter(
                onLongClick = { image ->
                    onTempImageLongClick(image)
                },
                onClick = { imageUri ->
                    onImageClick(imageUri)
                }
            )
            rvAddImage.adapter = rvAdapter

            sharedViewModel.imageList.observe(viewLifecycleOwner) { imageList ->
                if (imageList.isNotEmpty()) {
                    this.tvAddImageName.text =
                        "${imageList.size} ${if (imageList.size == 1) "image" else "images"} selected"
                    this.rvAddImage.visibility = View.VISIBLE
                    rvAdapter.submitList(imageList)
                } else {
                    this.tvAddImageName.text = getString(R.string.no_image_set_text)
                    this.rvAddImage.visibility = View.GONE
                }
            }

            tgAddDeadline.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    closeSoftKeyboard(binding.root, requireContext())
                    when (checkedId) {
                        R.id.btn_tg_add_duration -> {
                            tilAddExpirationDate.visibility = View.GONE
                            layoutAddDuration.visibility = View.VISIBLE
                        }
                        R.id.btn_tg_add_expiration_date -> {
                            layoutAddDuration.visibility = View.GONE
                            tilAddExpirationDate.visibility = View.VISIBLE
                        }
                    }
                }
            }

            if (model.expirationDate > DEFAULT_MODEL.expirationDate) {
                val durationPair = sharedViewModel.getDuration()
                tilEtAddDuration.setText(durationPair.first)
                actvAddUnit.setText(durationPair.second, false)
                tilEtAddExpirationDate.setText(formatDateMillis(model.expirationDate))
            }

            if (model.isValid()) {
                btnAddDelete.setOnClickListener {
                    showConfirmationDialog()
                }
                btnAddDelete.visibility = View.VISIBLE

                onSaveClick = {
                    sharedViewModel.updateWarranty()
                }
            } else {
                onSaveClick = {
                    //Using modifiedDate value in order to match the value between variable when inserted
                    sharedViewModel.inputModel = model.copy(createdDate = model.modifiedDate)
                    sharedViewModel.insertWarranty()
                }
            }

            btnAddSave.setOnClickListener {
                if (isInputValid()) {
                    model.warrantyName = inputWarrantyName
                    model.note = inputNote
                    model.modifiedDate = System.currentTimeMillis()
                    model.expirationDate = if (tgCheckedId == R.id.btn_tg_add_duration) {
                        sharedViewModel.calculateExpirationDate(inputDuration, inputUnit)
                    } else {
                        model.expirationDate
                    }
                    onSaveClick()
                    findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
                }
            }

            btnAddTakePicture.setOnClickListener {
                model.warrantyName = inputWarrantyName
                model.note = inputNote
                navigateToCamera()
            }

            btnAddPickImage.setOnClickListener {
                imageChooserActivity.launch("image/*")
            }

            if (sharedViewModel.getImageUriCount() > 0) {
                sharedViewModel.onCheckCameraImages()
            }

            tilEtAddPurchaseDate.setOnClickListener {
                val onPositiveClick: (Long) -> Unit = { date ->
                    model.purchaseDate = date
                    tilEtAddPurchaseDate.setText(formatDateMillis(model.purchaseDate))
                }

                showDatePicker(model.purchaseDate, onPositiveClick)
            }

            tilEtAddDuration.doAfterTextChanged {
                model.expirationDate = sharedViewModel.calculateExpirationDate(inputDuration, inputUnit)
            }

            actvAddUnit.doAfterTextChanged {
                model.expirationDate = sharedViewModel.calculateExpirationDate(inputDuration, inputUnit)
            }

            tilEtAddExpirationDate.setOnClickListener {
                val dateConstraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(nowMillis))
                    .build()
                val onPositiveClick: (Long) -> Unit = { date ->
                    model.expirationDate = date
                    tilEtAddExpirationDate.setText(formatDateMillis(model.expirationDate))
                }
                showDatePicker(model.expirationDate, onPositiveClick, dateConstraints)
            }
        }
    }

    private fun onTempImageLongClick(image: Image) {
        onTempImageDelete(image)
    }

    private fun onTempImageDelete(image: Image) {
        sharedViewModel.onTempImageDelete(image)
    }

    private fun onImageClick(imageUri: String) {
        val action = AddWarrantyFragmentDirections.actionAddWarrantyFragmentToImageViewFragment(
            imageUri = imageUri,
            imageName = getUriFilename(requireContext(), imageUri.toUri())
        )
        findNavController().navigate(action)
    }

//    private fun showImage(file: File) {
//        binding.apply {
//            this.tvAddImageName.visibility = View.GONE
//            this.imgAddImage.setImageURI(file.toUri())
//            this.imgAddImage.visibility = View.VISIBLE
//        }
//    }

    private fun navigateToCamera() {
        findNavController().navigate(R.id.action_addWarrantyFragment_to_cameraFragment)
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.delete_message_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteWarranty()
            }
            .show()
    }

    private fun deleteWarranty() {
        sharedViewModel.deleteWarranty()
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }


    private fun isInputValid(): Boolean {
        setError()
        return inputWarrantyName.isNotBlank() &&
                ((tgCheckedId == R.id.btn_tg_add_expiration_date && inputExpirationDate.isNotBlank() && model.expirationDate > model.purchaseDate) ||
                        tgCheckedId == R.id.btn_tg_add_duration && inputDuration.isNotBlank())
    }

    private fun setError() {
        binding.apply {
            tilAddWarrantyName.error = null
            tilAddDuration.error = null
            tilAddExpirationDate.error = null

            if (inputWarrantyName.isBlank()) {
                tilAddWarrantyName.error = getString(R.string.warranty_name_error_text)
            }

            if (tgCheckedId == R.id.btn_tg_add_duration && inputDuration.isBlank()) {
                tilAddDuration.error = getString(R.string.duration_error_text)
            } else if (tgCheckedId == R.id.btn_tg_add_expiration_date && inputExpirationDate.isBlank()) {
                tilAddExpirationDate.error = getString(R.string.purchase_expiration_date_error_text)
            }
        }
    }

    private fun showDatePicker(
        defaultDate: Long,
        onPositiveClick: (Long) -> Unit,
        constraints: CalendarConstraints = EMPTY_DATE_CONSTRAINT
    ) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraints)
                .setTitleText("Select date")
                .setSelection(
                    defaultDate.takeIf {
                        model.isValid()
                    } ?: getDefaultDateSelection().takeIf {
                        constraints != EMPTY_DATE_CONSTRAINT
                    } ?: (nowMillis)
                )
                .build()
        datePicker.apply {
            this.addOnPositiveButtonClickListener { date ->
                onPositiveClick(date)
            }
            this.isCancelable = false
            show(this@AddWarrantyFragment.requireActivity().supportFragmentManager, "tag")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
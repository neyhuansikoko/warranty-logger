package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.isValid
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private val inputWarrantyName: String get() = binding.tilEtAddWarrantyName.text.toString()
    private val inputNote: String get() = binding.tilEtAddNote.text.toString()
    private val inputPurchaseDate: String get() = binding.tilEtAddPurchaseDate.text.toString()
    private val inputDuration: String get() = binding.tilEtAddDuration.text.toString()
    private val inputUnit: String get() = binding.actvAddUnit.text.toString()
    private val inputExpirationDate: String get() = binding.tilEtAddExpirationDate.text.toString()
    private val tgCheckedId: Int get() = binding.tgAddDeadline.checkedButtonId
    private val imageChooserActivity = registerForActivityResult(ActivityResultContracts.GetContent()) {imageUri ->
        //Return content URI
        if (imageUri != null) {
            val inputStream = context?.contentResolver?.openInputStream(imageUri)
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            runBlocking {
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile(name, TEMP_IMAGE_SUFFIX, requireActivity().cacheDir)
                }
                file.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                sharedViewModel.tempImage = file.compressImage()
                sharedViewModel.tempImage?.let { showImage(it) }
                binding.imgAddImage.invalidate()
            }
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

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.list_item_unit,
                resources.getStringArray(R.array.time_unit_array_larger)
            )
            actvAddUnit.setAdapter(adapter)
            actvAddUnit.setText(adapter.getItem(0), false)

            tgAddDeadline.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    closeSoftKeyboard(binding.root, requireContext())
                    when (checkedId) {
                        R.id.btn_tg_add_duration -> {
                            tilAddExpirationDate.visibility = View.GONE
                            layoutAddDuration.visibility = View.VISIBLE
                        }
                        R.id.btn_tg_add_expiration_date -> {
                            tilAddExpirationDate.visibility = View.VISIBLE
                            layoutAddDuration.visibility = View.GONE
                        }
                    }
                }
            }

            tilEtAddExpirationDate.setText(if (model.expirationDate > DEFAULT_MODEL.expirationDate) {
                formatDateMillis(model.expirationDate)
            } else {
                getString(R.string.empty)
            })

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

            //Check if tempImage exist and load it, if not then try to load from modelWarranty
            sharedViewModel.tempImage?.let {
                showImage(it)
            } ?: model.image?.let { image ->
                getImageFile(requireActivity(), image)?.let {
                    showImage(it)
                }
            }

            tilEtAddPurchaseDate.setOnClickListener {
                val onPositiveClick: (Long) -> Unit = { date ->
                    model.purchaseDate = date
                    binding.tilEtAddPurchaseDate.setText(formatDateMillis(model.purchaseDate))
                }

                showDatePicker(model.purchaseDate, onPositiveClick)
            }

            tilEtAddExpirationDate.setOnClickListener {
                val dateConstraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(DEFAULT_DATE_SELECTION))
                    .build()
                val onPositiveClick: (Long) -> Unit = { date ->
                    model.expirationDate = date
                    binding.tilEtAddExpirationDate.setText(formatDateMillis(model.expirationDate))
                }
                showDatePicker(model.expirationDate, onPositiveClick, dateConstraints)
            }
        }
    }

    private fun showImage(file: File) {
        binding.apply {
            this.tvAddImageName.visibility = View.GONE
            this.imgAddImage.setImageURI(file.toUri())
            this.imgAddImage.visibility = View.VISIBLE
        }
    }

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
                ((tgCheckedId == R.id.btn_tg_add_expiration_date && inputExpirationDate.isNotBlank() && inputExpirationDate > inputPurchaseDate) ||
                        tgCheckedId == R.id.btn_tg_add_duration && inputDuration.isNotBlank())
    }

    private fun setError() {
        binding.apply {
            tilEtAddWarrantyName.error = null
            tilEtAddPurchaseDate.error = null
            tilEtAddDuration.error = null
            tilAddExpirationDate.error = null

            if (inputWarrantyName.isBlank()) {
                tilEtAddWarrantyName.error = getString(R.string.warranty_name_error_text)
            }

            if (tgCheckedId == R.id.btn_tg_add_duration && inputDuration.isBlank()) {
                tilEtAddDuration.error = getString(R.string.duration_error_text)
            } else if (tgCheckedId == R.id.btn_tg_add_expiration_date && inputExpirationDate.isBlank()) {
                tilEtAddExpirationDate.error = getString(R.string.purchase_expiration_date_error_text)
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
                        it > DEFAULT_MODEL.expirationDate
                    } ?: DEFAULT_DATE_SELECTION.takeIf {
                        constraints != EMPTY_DATE_CONSTRAINT
                    } ?: (MaterialDatePicker.todayInUtcMilliseconds())
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
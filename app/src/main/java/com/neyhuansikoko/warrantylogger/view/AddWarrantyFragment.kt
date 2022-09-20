package com.neyhuansikoko.warrantylogger.view

import android.app.ProgressDialog.show
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
    private val model get() = sharedViewModel.inputModel

    private var onSaveClick: () -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
            tilEtAddPurchaseDate.setText(if (model.purchaseDate > DEFAULT_MODEL.expirationDate) {
                formatDateMillis(model.purchaseDate)
            } else {
                getString(R.string.empty)
            })

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.list_item_unit,
                resources.getStringArray(R.array.time_unit_array_larger)
            )
            actvAddUnit.setAdapter(adapter)
            actvAddUnit.setText(adapter.getItem(0), false)

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
                    model.expirationDate = model.expirationDate.takeIf { it > DEFAULT_MODEL.expirationDate && inputDuration.isBlank() }
                        ?: sharedViewModel.calculateExpirationDate(inputDuration, inputUnit)
                    onSaveClick()
                    findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
                }
            }

            btnAddTakePicture.setOnClickListener {
                model.warrantyName = inputWarrantyName
                model.note = inputNote
                navigateToCamera()
            }

            //Check if tempImage exist and load it, if not then try to load from modelWarranty
            sharedViewModel.tempImage?.let {
                tvAddImageName.visibility = View.GONE
                imgAddImage.setImageURI(it.toUri())
                imgAddImage.visibility = View.VISIBLE
            } ?: model.image?.let { image ->
                getImageFile(requireActivity(), image)?.let {
                    tvAddImageName.visibility = View.GONE
                    imgAddImage.setImageURI(it.toUri())
                    imgAddImage.visibility = View.VISIBLE
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
                (inputExpirationDate.isNotBlank() || (inputPurchaseDate.isNotBlank() && inputDuration.isNotBlank())) &&
                ((model.purchaseDate < model.expirationDate).takeIf { inputPurchaseDate.isNotBlank() } ?: true)
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

            if (inputExpirationDate.isBlank()) {
                if (inputPurchaseDate.isBlank() || inputDuration.isBlank()) {
                    tilEtAddPurchaseDate.error = getString(R.string.purchase_date_error_text)
                    tilEtAddDuration.error = getString(R.string.duration_error_text)
                } else {
                    //trigger purchase date and expiration date update
                }
            } else if (inputPurchaseDate.isNotBlank()) {
                if (inputDuration.isBlank()) {
                    if (model.purchaseDate >= model.expirationDate) {
                        tilEtAddPurchaseDate.error = getString(R.string.purchase_expiration_date_error_text)
                        tilAddExpirationDate.error = getString(R.string.purchase_expiration_date_error_text)
                    } else {
                        //trigger purchase date update
                    }
                } else {
                    //trigger purchase and expiration date update
                }
            } else {
                //trigger expiration date update
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
                    } ?: (DEFAULT_DATE_SELECTION - DAY_MILLIS)
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
package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
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
                    sharedViewModel.insertWarranty()
                }
            }

            btnAddSave.setOnClickListener {
                if (!isTextFieldEmpty()) {
                    model.warrantyName = inputWarrantyName
                    onSaveClick()
                    findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
                }
            }

            btnAddTakePicture.setOnClickListener {
                model.warrantyName = inputWarrantyName
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

            tilEtAddExpirationDate.setOnClickListener {
                showDatePicker(model)
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

    private fun isTextFieldEmpty(): Boolean {
        setError()
        return inputWarrantyName.isBlank() || inputExpirationDate.isBlank()
    }

    private fun setError() {
        binding.apply {
            tilEtAddWarrantyName.error = if (inputWarrantyName.isBlank()) {
                getString(R.string.warranty_name_error_text)
            } else {
                null
            }
            tilEtAddExpirationDate.error = if (inputExpirationDate.isBlank()) {
                getString(R.string.warranty_name_error_text)
            } else {
                null
            }
        }
    }

    private fun showDatePicker(warranty: Warranty) {
        val dateConstraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(DEFAULT_DATE_SELECTION))
            .build()
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(dateConstraints)
                .setTitleText("Select date")
                .setSelection(
                    warranty.expirationDate.takeIf {
                        it > DEFAULT_MODEL.expirationDate
                    } ?: DEFAULT_DATE_SELECTION
                )
                .build()
        datePicker.apply {
            this.addOnPositiveButtonClickListener { date ->
                warranty.expirationDate = date
                binding.tilEtAddExpirationDate.setText(formatDateMillis(warranty.expirationDate))
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
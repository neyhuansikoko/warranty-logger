package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory
import java.io.File

class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val navigationArgs: AddWarrantyFragmentArgs by navArgs()
    private val warrantyId: Int  by lazy { navigationArgs.id }
    private val tempImage: File? by lazy { navigationArgs.image?.let { getImageFileFromCache(requireActivity(), it) } }
    private val inputWarrantyName by lazy { navigationArgs.inputWarrantyName }
    private val inputExpirationDate by lazy { navigationArgs.inputExpirationDate }

    private lateinit var warranty: Warranty

    private var defaultSelection: Long = 0
    private var expirationDateInMillis: Long = 0
    private var warrantyName: String = ""

    private val sharedViewModel: WarrantyViewModel by activityViewModels {
        WarrantyViewModelFactory((activity?.applicationContext as WarrantyLoggerApplication).database.warrantyDao())
    }

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

        if (warrantyId > 0) {
            setupEditWarranty()
        } else {
            setupAddWarranty()
        }

        binding.apply {
            btnAddCancel.setOnClickListener { findNavController().navigateUp() }

            defaultSelection = MaterialDatePicker.todayInUtcMilliseconds() + DAY_MILLIS

            tilEtAddExpirationDate.setOnClickListener {
                val dateConstraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds() + DAY_MILLIS))
                    .build()
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(dateConstraints)
                        .setTitleText("Select date")
                        .setSelection(defaultSelection)
                        .build()
                datePicker.apply {
                    addOnPositiveButtonClickListener { date ->
                        expirationDateInMillis = date
                        defaultSelection = expirationDateInMillis
                        binding.tilEtAddExpirationDate.setText(formatDateMillis(expirationDateInMillis))
                    }
                    isCancelable = false
                    show(this@AddWarrantyFragment.requireActivity().supportFragmentManager, "tag")
                }
            }
        }
    }

    private fun setupAddWarranty() {
        binding.apply {
            tilEtAddWarrantyName.setText(inputWarrantyName.ifEmpty { null })
            if (inputExpirationDate > 0) {
                expirationDateInMillis = inputExpirationDate
                tilEtAddExpirationDate.setText(formatDateMillis(expirationDateInMillis))
            }

            tempImage?.let {
                imgAddImage.setImageURI(it.toUri())
                tvAddImageName.text = it.name
            }
            btnAddSave.setOnClickListener {
                if (!isTextFieldEmpty()) {
                    addWarranty()
                }
            }
            btnAddTakePicture.setOnClickListener {
                warrantyName = binding.tilEtAddWarrantyName.text.toString()
                val action = AddWarrantyFragmentDirections.actionAddWarrantyFragmentToCameraFragment(
                    inputWarrantyName = warrantyName,
                    inputExpirationDate = expirationDateInMillis
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun setupEditWarranty() {
        sharedViewModel.getWarrantyById(warrantyId).observe(viewLifecycleOwner) {
            it?.let {
                warranty = it
                bind(warranty)

                binding.apply {
                    btnAddSave.setOnClickListener {
                        if (!isTextFieldEmpty()) {
                            updateWarranty()
                        }
                    }

                    btnAddDelete.setOnClickListener {
                        showConfirmationDialog()
                    }
                    btnAddDelete.visibility = View.VISIBLE

                    btnAddTakePicture.setOnClickListener {
                        warrantyName = binding.tilEtAddWarrantyName.text.toString()
                        val action = AddWarrantyFragmentDirections.actionAddWarrantyFragmentToCameraFragment(
                            id = warrantyId,
                            inputWarrantyName = warrantyName,
                            inputExpirationDate = expirationDateInMillis
                        )
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun bind(warranty: Warranty) {
        binding.apply {
            tilEtAddWarrantyName.setText(inputWarrantyName.ifEmpty { warranty.warrantyName })
            expirationDateInMillis = if (inputExpirationDate > 0) {
                defaultSelection = inputExpirationDate
                defaultSelection
            } else {
                defaultSelection = warranty.expirationDate
                defaultSelection
            }
            tilEtAddExpirationDate.setText(formatDateMillis(expirationDateInMillis))

            //Set image and image name, if it exist
            if (navigationArgs.image != null) {
                val tempImage = navigationArgs.image!!
                imgAddImage.setImageURI(getImageFileFromCache(requireActivity(), tempImage).toUri())
                tvAddImageName.text = tempImage
            } else if (warranty.image != null) {
                val image = warranty.image!!
                imgAddImage.setImageURI(getImageFile(requireActivity(), image).toUri())
                tvAddImageName.text = image
            }
        }
    }

    private fun addWarranty() {
        val newImage = saveTempImage()

        sharedViewModel.addNewWarranty(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            image = newImage?.name
        )
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun updateWarranty() {
        val newImage = saveTempImage()

        //Delete old image if user have taken a new image when clicking save and if the warranty have an old image
        if (warranty.image != null && newImage != null) {
            val image = warranty.image!!
            getImageFile(requireActivity(), image).delete()
        }
        val updatedWarranty = warranty.copy(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            image = newImage?.name ?: warranty.image // if newImage is null, then warranty.image. if warranty.image is null, then null
        )
        sharedViewModel.updateWarranty(updatedWarranty)
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun deleteWarranty() {
        warranty.image?.let { getImageFile(requireActivity(), it).delete() }
        sharedViewModel.deleteWarranty(warranty)
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
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

    private fun isTextFieldEmpty(): Boolean {
        binding.apply {
            return if (!tilEtAddWarrantyName.text.isNullOrBlank() && expirationDateInMillis > 0) {
                tilAddWarrantyName.error = null
                tilAddExpirationDate.error = null
                false
            } else {
                if (tilEtAddWarrantyName.text.isNullOrBlank()) {
                    tilAddWarrantyName.error = getString(R.string.warranty_name_error_text)
                }
                if (tilEtAddExpirationDate.text.isNullOrBlank()) {
                    tilAddExpirationDate.error = getString(R.string.expiration_date_error_text)
                }
                true
            }
        }
    }

    //Copy temp image to non-cache directory
    private fun saveTempImage(): File? {
        var newImage: File? = null
        tempImage?.let { temp ->
            newImage = getImageFile(requireActivity(), temp.name)
            temp.copyTo(newImage!!)
            temp.delete()
        }
        return newImage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tempImage?.delete()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        tempImage?.delete()
    }
}
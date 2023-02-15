package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.adapter.ImageListAdapter
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.database.model.Warranty
import com.neyhuansikoko.warrantylogger.database.model.getRemainingDate
import com.neyhuansikoko.warrantylogger.database.model.isValid
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyDetailBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarrantyDetailFragment : Fragment() {

    private var _binding: FragmentWarrantyDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private lateinit var arrayAdapter: ArrayAdapter<String>

    private lateinit var rvAdapter: ImageListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentWarrantyDetailBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.displayModel.observe(viewLifecycleOwner) {
            if (it.isValid()) {
                bindModel(it)
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun bindModel(warranty: Warranty) {
        binding.apply {
            tvDetailWarrantyName.text = warranty.warrantyName
            tvDetailNote.text = warranty.note.takeIf { it.isNotBlank() } ?: getString(R.string.no_data_text)
            tvDetailCreatedDate.text = formatDateTimeMillis(warranty.createdDate)
            tvDetailModifiedDate.text = formatDateTimeMillis(warranty.modifiedDate)
            tvDetailPurchaseDate.text = if (warranty.purchaseDate > Long.MIN_VALUE) {
                formatDateMillis(warranty.purchaseDate)
            } else {
                getString(R.string.no_information_text)
            }
            tvDetailExpirationDate.text = formatDateMillis(warranty.expirationDate)
            tvDetailRemainingTime.text = warranty.getRemainingDate()

            arrayAdapter = ArrayAdapter(requireContext(), R.layout.list_item_unit, resources.getStringArray(R.array.time_unit_array))
            actvDetailUnit.setAdapter(arrayAdapter)

            rvAdapter = ImageListAdapter(
                onLongClick = { image ->
                    onImageLongClick(image)
                },
                onClick = { imageUri ->
                    onImageClick(imageUri)
                }
            )
            rvDetailImage.adapter = rvAdapter

            sharedViewModel.warrantyImageList.observe(viewLifecycleOwner) { imageList ->
                if (imageList.isNotEmpty()) {
                    this.tvDetailImageName.text =
                        "${imageList.size} ${if (imageList.size == 1) "image" else "images"} saved"
                } else {
                    this.tvDetailImageName.text = getString(R.string.no_image_set_text)
                }
                rvAdapter.submitList(imageList)
            }

            btnDetailApply.setOnClickListener { view ->
                closeSoftKeyboard(binding.root, requireContext())
                setReminder(view, warranty)
            }

            btnDetailReset.setOnClickListener {
                resetReminderDuration()
            }
            //Initiate default value for reminder customization
            resetReminderDuration()

            toggleBtnDetail.clearChecked()
            toggleBtnDetail.addOnButtonCheckedListener { _, _, isChecked ->
                if (isChecked) {
                    cardDetailReminder.visibility = View.VISIBLE
                } else {
                    closeSoftKeyboard(binding.root, requireContext())
                    cardDetailReminder.visibility = View.GONE
                }
            }

            lifecycleScope.launch(Dispatchers.Default) {
                sharedViewModel.onCheckWorkExist()
            }

            sharedViewModel.workExist.observe(viewLifecycleOwner) { exist ->
                if (getDaysFromDateMillis(warranty.expirationDate) > 1) {
                    switchDetailReminder.isEnabled = true
                    btnDetailCustomizeReminder.isEnabled = true

                    switchDetailReminder.isChecked = exist

                    switchDetailReminder.setOnClickListener { view ->
                        if (switchDetailReminder.isChecked) {
                            val delayTime = setReminder(view, warranty)

                            if (delayTime < 1) { switchDetailReminder.isChecked = false }
                        } else {
                            sharedViewModel.cancelWork()
                            Snackbar.make(
                                switchDetailReminder,
                                "Reminder has been cancelled",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        sharedViewModel.onCheckWarrantyImages(warranty.warrantyId)
        setupOptionMenu()
    }

    private fun onImageLongClick(image: Image) {
        showDeleteImageConfirmationDialog(image)
    }

    private fun showDeleteImageConfirmationDialog(image: Image) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.delete_message_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                onPersistImageDelete(image)
            }
            .show()
    }

    private fun onPersistImageDelete(image: Image) {
        sharedViewModel.onPersistImageDelete(image)
    }

    private fun onImageClick(imageUri: String) {
        val action = WarrantyDetailFragmentDirections.actionWarrantyDetailFragmentToImageViewFragment(
            imageUri = imageUri,
            imageName = getUriFilename(requireContext(), imageUri.toUri())
        )
        findNavController().navigate(action)
    }

    private fun setReminder(view: View, warranty: Warranty): Long {
        binding.apply {
            val duration = tilEtDetailDuration.text.toString()
            val timeUnit = actvDetailUnit.text.toString()
            val delayTime = sharedViewModel.scheduleReminder(warranty, duration, timeUnit)

            if (duration.toLong() > 1) {
                if (delayTime > 0) {
                    Snackbar.make(
                        view,
                        "Reminder has been set to $delayTime days from now",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    switchDetailReminder.isChecked = true
                } else {
                    Snackbar.make(
                        view,
                        "Cannot set the reminder at the specified time",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } else {
                Snackbar.make(
                    view,
                    "Cannot set duration to be smaller than 1",
                    Snackbar.LENGTH_SHORT
                ).show()

                return 0
            }

            return delayTime
        }
    }

    //Set default value for reminder customization
    private fun resetReminderDuration() {
        binding.apply {
            tilEtDetailDuration.setText(DEFAULT_DURATION.toString())
            actvDetailUnit.setText(arrayAdapter.getItem(0), false)
        }
    }


    private fun setupOptionMenu() {
        //Create option menu
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_item_edit -> {
                        editWarranty()
                        true
                    }
                    R.id.menu_item_delete -> {
                        showDeleteWarrantyConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun editWarranty() {
        val action = WarrantyDetailFragmentDirections.actionWarrantyDetailFragmentToAddWarrantyFragment(title = getString(R.string.edit_warranty_title_text))
        findNavController().navigate(action)
    }

    private fun showDeleteWarrantyConfirmationDialog() {
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
        findNavController().navigate(R.id.action_warrantyDetailFragment_to_warrantyListFragment)
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.clearTempImage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
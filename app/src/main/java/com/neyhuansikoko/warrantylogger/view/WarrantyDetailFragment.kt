package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import android.widget.Adapter
import android.widget.ArrayAdapter
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.getRemainingTime
import com.neyhuansikoko.warrantylogger.database.isValid
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyDetailBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WarrantyDetailFragment : Fragment() {

    private var _binding: FragmentWarrantyDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private lateinit var arrayAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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
            tvDetailExpirationDate.text = formatDateMillis(warranty.expirationDate)
            tvDetailRemainingTime.text = warranty.getRemainingTime()

            //Set image and image name, if it exist
            warranty.image?.let { image ->
                getImageFile(requireActivity(), image)?.let {
                    tvDetailImageName.visibility = View.GONE
                    imgDetailImage.setImageURI(it.toUri())
                    imgDetailImage.visibility = View.VISIBLE
                }
            }

            arrayAdapter = ArrayAdapter(requireContext(), R.layout.list_item_unit, resources.getStringArray(R.array.time_unit_array))
            actvDetailUnit.setAdapter(arrayAdapter)

            if (getDaysToDate(warranty.expirationDate) > 1) {
                switchDetailReminder.visibility = View.VISIBLE
                toggleBtnDetail.visibility = View.VISIBLE
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

            runBlocking {
                val workExist = async { sharedViewModel.doesWorkExist() }
                switchDetailReminder.isChecked = workExist.await()

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
        setupOptionMenu()
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
                        showConfirmationDialog()
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
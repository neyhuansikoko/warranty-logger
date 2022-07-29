package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.getRemainingTime
import com.neyhuansikoko.warrantylogger.database.isValid
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyDetailBinding
import com.neyhuansikoko.warrantylogger.formatDateMillis
import com.neyhuansikoko.warrantylogger.getImageFile
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WarrantyDetailFragment : Fragment() {

    private var _binding: FragmentWarrantyDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

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
                    imgDetailImage.setImageURI(it.toUri())
                    tvDetailImageName.text = it.name
                }
            }
        }

        setupOptionMenu()
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
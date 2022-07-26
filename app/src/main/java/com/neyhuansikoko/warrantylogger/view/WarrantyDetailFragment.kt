package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyDetailBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory
import kotlinx.coroutines.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WarrantyDetailFragment : Fragment() {

    private var _binding: FragmentWarrantyDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val navigationArgs: WarrantyDetailFragmentArgs by navArgs()

    private val sharedViewModel: WarrantyViewModel by activityViewModels {
        WarrantyViewModelFactory((activity?.applicationContext as WarrantyLoggerApplication).database.warrantyDao())
    }

    private lateinit var warranty: Warranty

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWarrantyDetailBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val warrantyId = navigationArgs.id

        sharedViewModel.getWarrantyById(warrantyId).observe(viewLifecycleOwner) {
            it?.let {
                warranty = it
                bind(warranty)
            }
        }

        //Create option menu
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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

    private fun bind(warranty: Warranty) {
        binding.apply {
            tvDetailWarrantyName.text = warranty.warrantyName
            tvDetailExpirationDate.text = formatDateMillis(warranty.expirationDate)
            if (warranty.imageUri != null) {
                imgDetailImage.setImageURI(warranty.imageUri.toUri())
                tvDetailImageName.text = getFileNameFromUri(warranty.imageUri.toUri(), requireActivity()) ?: getString(
                    R.string.no_image_set_text)
            } else {
                tvDetailImageName.text = getString(R.string.no_image_set_text)
            }
        }
    }

    private fun editWarranty() {
        val action = WarrantyDetailFragmentDirections.actionWarrantyDetailFragmentToAddWarrantyFragment(id = warranty.id, title = getString(R.string.edit_warranty_title_text))
        findNavController().navigate(action)
    }

    private fun deleteWarranty() {
        sharedViewModel.deleteWarranty(warranty)
        deleteFileByUri(warranty.imageUri!!.toUri(), requireActivity())
        findNavController().navigate(R.id.action_warrantyDetailFragment_to_warrantyListFragment)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
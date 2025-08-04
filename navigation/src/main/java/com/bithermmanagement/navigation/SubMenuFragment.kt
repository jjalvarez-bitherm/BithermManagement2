package com.bithermmanagement.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bithermmanagement.navigation.databinding.FragmentSubMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubMenuFragment : Fragment() {

    private var _binding: FragmentSubMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavigationViewModel by activityViewModels()
    private val args: SubMenuFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainMenuName = args.mainMenuName
        binding.tvSubmenuTitle.text = mainMenuName

        binding.ivBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        val subMenuAdapter = MainMenuAdapter(false) {
            // Lógica de clic para submenús si es necesaria
        }
        
        binding.rvSubmenu.layoutManager = GridLayoutManager(context, 2)
        binding.rvSubmenu.adapter = subMenuAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadSubMenuItems(args.mainMenuKey).collect { subItems ->
                subMenuAdapter.submitList(subItems)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
package com.bithermmanagement.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bithermmanagement.navigation.databinding.FragmentMainMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavigationViewModel by activityViewModels()
    private lateinit var mainMenuAdapter: MainMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBackArrow.visibility = View.GONE // Ocultar la flecha en el menú principal

        setupRecyclerView()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuItems.collect { allMenuItems ->
                val mainMenus = allMenuItems.filter { it.parentId == "0" }
                (binding.rvMainMenu.adapter as? MainMenuAdapter)?.submitList(mainMenus)
            }
        }
    }

    private fun setupRecyclerView() {
        mainMenuAdapter = MainMenuAdapter(true) { menuEntity ->
            val action = MainMenuFragmentDirections.actionMainMenuFragmentToSubMenuFragment(
                mainMenuKey = menuEntity.id,
                mainMenuName = menuEntity.visibleName
            )
            findNavController().navigate(action)
        }

        val layoutManager = GridLayoutManager(requireContext(), 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (mainMenuAdapter.itemCount > position && mainMenuAdapter.currentList[position].id == "Favoritos") 2 else 1
            }
        }

        binding.rvMainMenu.layoutManager = layoutManager
        binding.rvMainMenu.adapter = mainMenuAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
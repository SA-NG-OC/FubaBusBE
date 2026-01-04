package com.example.Fuba_BE.service.Driver;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService implements IDriverService {

    private final DriverRepository driverRepository;
    private final SelectionMapper selectionMapper;

    @Override
    public List<DriverSelectionDTO> getAllDriversForSelection() {
        // Lấy tất cả tài xế
        List<Driver> drivers = driverRepository.findAll();

        return drivers.stream()
                .map(selectionMapper::toDriverSelectionDTO)
                .toList();
    }
}

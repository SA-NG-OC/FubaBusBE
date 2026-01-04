package com.example.Fuba_BE.service.Driver;

import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;

import java.util.List;

public interface IDriverService {
    List<DriverSelectionDTO> getAllDriversForSelection();
}

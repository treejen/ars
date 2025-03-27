package com.hktv.ars.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.service.AhocorasickService;
import com.hktv.ars.service.PythonService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


@Service
@RequiredArgsConstructor
public class PythonServiceImpl implements PythonService {

    private final AhocorasickService ahocorasickService;

    @Override
    public RegionResponseData extractAddresses(String address) {
        RegionResponseData regionResponseData = new RegionResponseData();
        regionResponseData.setAddress(address);
        try {
            String scriptPath = "src/main/resources/scripts/analyze_address.py";

            // 確保 Python 腳本存在
            File script = new File(scriptPath);
            if (!script.exists()) {
                System.out.println("Python script not found: " + script.getAbsolutePath());
                return regionResponseData;
            }

            // Windows 需要用 cmd /c
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "C:\\Python313\\python.exe", script.getAbsolutePath(), address);

            // 設定 Python 腳本所在的資料夾
            processBuilder.directory(new File("src/main/resources/scripts"));

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "Big5"));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // Capture errors
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(),"Big5"));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("Python Error: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Exited with code: " + exitCode);
            }

            if(output.isEmpty()){
                return regionResponseData;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            regionResponseData = objectMapper.readValue(output.toString(), RegionResponseData.class);
            return ahocorasickService.getDeliveryZoneCode(regionResponseData);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}

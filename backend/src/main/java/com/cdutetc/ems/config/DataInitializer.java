package com.cdutetc.ems.config;

import com.cdutetc.ems.entity.*;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.entity.enums.DeviceStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.entity.enums.UserRole;
import com.cdutetc.ems.entity.enums.UserStatus;
import com.cdutetc.ems.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据初始化器
 * 在应用启动时创建基础数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ems.data.initialize", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RadiationDeviceDataRepository radiationDeviceDataRepository;
    private final EnvironmentDeviceDataRepository environmentDeviceDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("开始初始化EMS系统基础数据...");

        try {
            // 初始化企业数据
            Company defaultCompany = initializeDefaultCompany();
            log.info("企业初始化完成: {}", defaultCompany.getCompanyName());

            // 初始化用户数据
            initializeDefaultUsers(defaultCompany);
            log.info("用户初始化完成");

            // 初始化示例设备
            initializeSampleDevices(defaultCompany);
            log.info("设备初始化完成");

            log.info("EMS系统基础数据初始化完成！");

        } catch (Exception e) {
            log.error("数据初始化过程中发生错误", e);
            throw e;
        }
    }

    /**
     * 初始化默认企业
     */
    private Company initializeDefaultCompany() {
        if (companyRepository.count() > 0) {
            log.info("企业数据已存在，跳过初始化");
            return companyRepository.findAll().get(0);
        }

        Company company = new Company();
        company.setCompanyCode("DEFAULT_COMPANY");
        company.setCompanyName("默认测试企业");
        company.setContactEmail("admin@ems.com");
        company.setContactPhone("13800138000");
        company.setAddress("测试地址");
        company.setDescription("EMS系统默认测试企业");
        company.setStatus(CompanyStatus.ACTIVE);

        Company savedCompany = companyRepository.save(company);
        log.info("创建默认企业: {} (ID: {})", savedCompany.getCompanyName(), savedCompany.getId());

        return savedCompany;
    }

    /**
     * 初始化默认用户
     */
    private void initializeDefaultUsers(Company company) {
        if (userRepository.count() > 0) {
            log.info("用户数据已存在，跳过初始化");
            return;
        }

        // 创建管理员用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@ems.com");
        admin.setFullName("系统管理员");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCompany(company);

        User savedAdmin = userRepository.save(admin);
        log.info("创建管理员用户: {} (ID: {})", savedAdmin.getUsername(), savedAdmin.getId());

        // 创建普通用户
        User normalUser = new User();
        normalUser.setUsername("user");
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setEmail("user@ems.com");
        normalUser.setFullName("测试用户");
        normalUser.setRole(UserRole.USER);
        normalUser.setStatus(UserStatus.ACTIVE);
        normalUser.setCompany(company);

        User savedNormalUser = userRepository.save(normalUser);
        log.info("创建普通用户: {} (ID: {})", savedNormalUser.getUsername(), savedNormalUser.getId());
    }

    /**
     * 初始化示例设备
     */
    private void initializeSampleDevices(Company company) {
        if (deviceRepository.count() > 0) {
            log.info("设备数据已存在，跳过初始化");
            return;
        }

        // 创建辐射监测设备
        Device radiationDevice = new Device();
        radiationDevice.setDeviceCode("RAD001");
        radiationDevice.setDeviceName("辐射监测仪001");
        radiationDevice.setDeviceType(DeviceType.RADIATION_MONITOR);
        radiationDevice.setStatus(DeviceStatus.ONLINE);
        radiationDevice.setLocation("办公区A栋");
        radiationDevice.setDescription("用于办公区辐射监测");
        radiationDevice.setManufacturer("测试设备厂商");
        radiationDevice.setModel("RAD-Monitor-v1");
        radiationDevice.setSerialNumber("RAD-SN-001");
        radiationDevice.setInstallDate(LocalDateTime.now().minusMonths(1));
        radiationDevice.setLastOnlineAt(LocalDateTime.now());
        radiationDevice.setCompany(company);
        radiationDevice.setPositionX(30);  // 3D场景位置: -8
        radiationDevice.setPositionY(30);  // 3D场景位置: -8

        Device savedRadiationDevice = deviceRepository.save(radiationDevice);
        log.info("创建辐射监测设备: {} (ID: {})", savedRadiationDevice.getDeviceCode(), savedRadiationDevice.getId());

        // 创建环境监测设备
        Device environmentDevice = new Device();
        environmentDevice.setDeviceCode("ENV001");
        environmentDevice.setDeviceName("环境监测站001");
        environmentDevice.setDeviceType(DeviceType.ENVIRONMENT_STATION);
        environmentDevice.setStatus(DeviceStatus.ONLINE);
        environmentDevice.setLocation("厂区B栋");
        environmentDevice.setDescription("用于厂区环境监测");
        environmentDevice.setManufacturer("测试设备厂商");
        environmentDevice.setModel("ENV-Station-v1");
        environmentDevice.setSerialNumber("ENV-SN-001");
        environmentDevice.setInstallDate(LocalDateTime.now().minusMonths(1));
        environmentDevice.setLastOnlineAt(LocalDateTime.now());
        environmentDevice.setCompany(company);
        environmentDevice.setPositionX(70);  // 3D场景位置: +8
        environmentDevice.setPositionY(50);  // 3D场景位置: 0

        Device savedEnvironmentDevice = deviceRepository.save(environmentDevice);
        log.info("创建环境监测设备: {} (ID: {})", savedEnvironmentDevice.getDeviceCode(), savedEnvironmentDevice.getId());

        // 创建第二个辐射设备（用于测试多设备场景）
        Device radiationDevice2 = new Device();
        radiationDevice2.setDeviceCode("RAD002");
        radiationDevice2.setDeviceName("辐射监测仪002");
        radiationDevice2.setDeviceType(DeviceType.RADIATION_MONITOR);
        radiationDevice2.setStatus(DeviceStatus.OFFLINE);
        radiationDevice2.setLocation("办公区C栋");
        radiationDevice2.setDescription("用于办公区C栋辐射监测");
        radiationDevice2.setManufacturer("测试设备厂商");
        radiationDevice2.setModel("RAD-Monitor-v1");
        radiationDevice2.setSerialNumber("RAD-SN-002");
        radiationDevice2.setInstallDate(LocalDateTime.now().minusMonths(2));
        radiationDevice2.setLastOnlineAt(LocalDateTime.now().minusHours(1));
        radiationDevice2.setCompany(company);
        radiationDevice2.setPositionX(50);  // 3D场景位置: 0 (中心)
        radiationDevice2.setPositionY(70);  // 3D场景位置: +8

        Device savedRadiationDevice2 = deviceRepository.save(radiationDevice2);
        log.info("创建辐射监测设备2: {} (ID: {})", savedRadiationDevice2.getDeviceCode(), savedRadiationDevice2.getId());
    }

    /**
     * 初始化示例设备监测数据
     */
    private void initializeSampleDeviceData() {
        if (radiationDeviceDataRepository.count() > 0 || environmentDeviceDataRepository.count() > 0) {
            log.info("设备监测数据已存在，跳过初始化");
            return;
        }

        // 创建辐射设备示例数据
        List<RadiationDeviceData> radiationDataList = createSampleRadiationData();
        List<RadiationDeviceData> savedRadiationData = radiationDeviceDataRepository.saveAll(radiationDataList);
        log.info("创建辐射设备示例数据: {} 条", savedRadiationData.size());

        // 创建环境设备示例数据
        List<EnvironmentDeviceData> environmentDataList = createSampleEnvironmentData();
        List<EnvironmentDeviceData> savedEnvironmentData = environmentDeviceDataRepository.saveAll(environmentDataList);
        log.info("创建环境设备示例数据: {} 条", savedEnvironmentData.size());
    }

    /**
     * 创建辐射设备示例数据
     */
    private List<RadiationDeviceData> createSampleRadiationData() {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);
        List<RadiationDeviceData> dataList = new java.util.ArrayList<>();

        // 为RAD001创建最近24小时的数据，每小时一条
        for (int i = 0; i < 24; i++) {
            LocalDateTime recordTime = baseTime.plusHours(i);

            // 模拟真实的CPM值变化
            double cpm = 15 + Math.random() * 10; // 15-25之间的随机值

            RadiationDeviceData data = new RadiationDeviceData();
            data.setDeviceCode("RAD001");
            data.setRawData(String.format(
                    "{\"src\":\"RAD001\",\"msgtype\":\"data\",\"CPM\":%d,\"Batvolt\":3.7,\"time\":\"%s\"}",
                    cpm, recordTime.toString()
            ));
            data.setSrc(1); // 假设1表示设备源
            data.setMsgtype(1); // 假设1表示data类型
            data.setCpm(cpm);
            data.setBatvolt(3.7 + Math.random() * 0.3); // 3.7-4.0之间的随机电压
            data.setTime(recordTime.toString());
            data.setRecordTime(recordTime);
            data.setTrigger(1);
            data.setMulti(1);
            data.setWay(1);
            data.setBdsLongitude(String.valueOf(116.4074 + Math.random() * 0.01)); // 北京经纬度附近
            data.setBdsLatitude(String.valueOf(39.9042 + Math.random() * 0.01));
            data.setBdsUtc(recordTime.toString());
            data.setLbsLongitude(String.valueOf(116.4074 + Math.random() * 0.01));
            data.setLbsLatitude(String.valueOf(39.9042 + Math.random() * 0.01));
            data.setLbsUseful(0); // 假设0表示LBS不可用

            dataList.add(data);
        }

        // 为RAD002创建少量数据（离线设备）
        for (int i = 0; i < 5; i++) {
            LocalDateTime recordTime = baseTime.plusHours(i * 4);
            double cpm = 12 + Math.random() * 8;

            RadiationDeviceData data = new RadiationDeviceData();
            data.setDeviceCode("RAD002");
            data.setRawData(String.format(
                    "{\"src\":\"RAD002\",\"msgtype\":\"data\",\"CPM\":%d,\"Batvolt\":3.6,\"time\":\"%s\"}",
                    cpm, recordTime.toString()
            ));
            data.setSrc(2); // 假设2表示设备源
            data.setMsgtype(1); // 假设1表示data类型
            data.setCpm(cpm);
            data.setBatvolt(3.6);
            data.setTime(recordTime.toString());
            data.setRecordTime(recordTime);
            data.setTrigger(1);
            data.setMulti(1);
            data.setWay(1);

            dataList.add(data);
        }

        return dataList;
    }

    /**
     * 创建环境设备示例数据
     */
    private List<EnvironmentDeviceData> createSampleEnvironmentData() {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(12);
        List<EnvironmentDeviceData> dataList = new java.util.ArrayList<>();

        // 为ENV001创建最近12小时的数据，每小时一条
        for (int i = 0; i < 12; i++) {
            LocalDateTime recordTime = baseTime.plusHours(i);

            // 模拟真实的环境数据变化
            double cpm = 20 + Math.random() * 15; // 20-35之间的随机CPM值
            double temperature = 20 + Math.random() * 10; // 20-30度
            double humidity = 40 + Math.random() * 30; // 40-70%湿度
            double windSpeed = Math.random() * 5; // 0-5 m/s风速
            double total = cpm * 0.8 + temperature * 0.1 + humidity * 0.05; // 综合环境指数
            double battery = 3.5 + Math.random() * 0.8; // 3.5-4.3V电池电压

            EnvironmentDeviceData data = new EnvironmentDeviceData();
            data.setDeviceCode("ENV001");
            data.setRawData(String.format(
                    "{\"src\":\"ENV001\",\"CPM\":%d,\"temperature\":%.1f,\"wetness\":%.1f,\"windspeed\":%.1f,\"total\":%.1f,\"battery\":%.1f}",
                    cpm, temperature, humidity, windSpeed, total, battery
            ));
            data.setSrc(3); // 假设3表示环境设备源
            data.setCpm(cpm);
            data.setTemperature(temperature);
            data.setWetness(humidity);
            data.setWindspeed(windSpeed);
            data.setTotal(total);
            data.setBattery(battery);
            data.setRecordTime(recordTime);

            dataList.add(data);
        }

        return dataList;
    }
}
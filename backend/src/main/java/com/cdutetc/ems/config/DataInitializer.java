package com.cdutetc.ems.config;

import com.cdutetc.ems.entity.*;
import com.cdutetc.ems.entity.enums.ActivationCodeStatus;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.entity.enums.DeviceActivationStatus;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 数据初始化器
 * 在应用启动时创建基础数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ems.data.initialize", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RadiationDeviceDataRepository radiationDeviceDataRepository;
    private final EnvironmentDeviceDataRepository environmentDeviceDataRepository;
    private final DeviceActivationCodeRepository activationCodeRepository;
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

            // 初始化设备监测数据
            initializeSampleDeviceData();
            log.info("设备监测数据初始化完成");

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
        radiationDevice.setActivationStatus(DeviceActivationStatus.ACTIVE);
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
        environmentDevice.setActivationStatus(DeviceActivationStatus.ACTIVE);
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
        radiationDevice2.setActivationStatus(DeviceActivationStatus.ACTIVE);
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

        // ========== Node-RED 测试设备 ==========
        // 创建RAD-001（Node-RED模拟的辐射设备，带连字符）
        Device rad001 = new Device();
        rad001.setDeviceCode("RAD-001");
        rad001.setDeviceName("Node-RED辐射监测仪001");
        rad001.setDeviceType(DeviceType.RADIATION_MONITOR);
        rad001.setActivationStatus(DeviceActivationStatus.ACTIVE);
        rad001.setStatus(DeviceStatus.ONLINE);
        rad001.setLocation("Node-RED模拟区域A");
        rad001.setDescription("Node-RED设备模拟器测试设备");
        rad001.setManufacturer("Node-RED Simulator");
        rad001.setModel("RAD-SIM-v1");
        rad001.setSerialNumber("RAD-SIM-001");
        rad001.setInstallDate(LocalDateTime.now().minusDays(1));
        rad001.setLastOnlineAt(LocalDateTime.now());
        rad001.setCompany(company);
        rad001.setPositionX(20);
        rad001.setPositionY(20);

        Device savedRad001 = deviceRepository.save(rad001);
        log.info("创建Node-RED测试设备: {} (ID: {})", savedRad001.getDeviceCode(), savedRad001.getId());

        // 创建ENV-001（Node-RED模拟的环境设备，带连字符）
        Device env001 = new Device();
        env001.setDeviceCode("ENV-001");
        env001.setDeviceName("Node-RED环境监测站001");
        env001.setDeviceType(DeviceType.ENVIRONMENT_STATION);
        env001.setActivationStatus(DeviceActivationStatus.ACTIVE);
        env001.setStatus(DeviceStatus.ONLINE);
        env001.setLocation("Node-RED模拟区域B");
        env001.setDescription("Node-RED设备模拟器测试设备");
        env001.setManufacturer("Node-RED Simulator");
        env001.setModel("ENV-SIM-v1");
        env001.setSerialNumber("ENV-SIM-001");
        env001.setInstallDate(LocalDateTime.now().minusDays(1));
        env001.setLastOnlineAt(LocalDateTime.now());
        env001.setCompany(company);
        env001.setPositionX(80);
        env001.setPositionY(80);

        Device savedEnv001 = deviceRepository.save(env001);
        log.info("创建Node-RED测试设备: {} (ID: {})", savedEnv001.getDeviceCode(), savedEnv001.getId());

        // ========== 未激活设备（用于测试激活功能） ==========
        // 创建待激活的辐射设备
        Device pendingRad1 = new Device();
        pendingRad1.setDeviceCode("RAD-PENDING-001");
        pendingRad1.setDeviceName("待激活辐射监测仪001");
        pendingRad1.setDeviceType(DeviceType.RADIATION_MONITOR);
        pendingRad1.setActivationStatus(DeviceActivationStatus.PENDING);
        pendingRad1.setStatus(DeviceStatus.OFFLINE);
        pendingRad1.setLocation("待安装区域A");
        pendingRad1.setDescription("待激活设备，用于测试激活功能");
        pendingRad1.setManufacturer("测试设备厂商");
        pendingRad1.setModel("RAD-Monitor-v2");
        pendingRad1.setSerialNumber("RAD-PEND-001");
        pendingRad1.setInstallDate(null);
        pendingRad1.setLastOnlineAt(null);
        pendingRad1.setCompany(company);

        Device savedPendingRad1 = deviceRepository.save(pendingRad1);
        log.info("创建待激活辐射设备: {} (ID: {})", savedPendingRad1.getDeviceCode(), savedPendingRad1.getId());
        createActivationCode(savedPendingRad1);

        // 创建待激活的环境设备
        Device pendingEnv1 = new Device();
        pendingEnv1.setDeviceCode("ENV-PENDING-001");
        pendingEnv1.setDeviceName("待激活环境监测站001");
        pendingEnv1.setDeviceType(DeviceType.ENVIRONMENT_STATION);
        pendingEnv1.setActivationStatus(DeviceActivationStatus.PENDING);
        pendingEnv1.setStatus(DeviceStatus.OFFLINE);
        pendingEnv1.setLocation("待安装区域B");
        pendingEnv1.setDescription("待激活设备，用于测试激活功能");
        pendingEnv1.setManufacturer("测试设备厂商");
        pendingEnv1.setModel("ENV-Station-v2");
        pendingEnv1.setSerialNumber("ENV-PEND-001");
        pendingEnv1.setInstallDate(null);
        pendingEnv1.setLastOnlineAt(null);
        pendingEnv1.setCompany(company);

        Device savedPendingEnv1 = deviceRepository.save(pendingEnv1);
        log.info("创建待激活环境设备: {} (ID: {})", savedPendingEnv1.getDeviceCode(), savedPendingEnv1.getId());
        createActivationCode(savedPendingEnv1);

        // 创建更多待激活设备（批量测试）
        for (int i = 2; i <= 5; i++) {
            Device pendingDevice = new Device();
            pendingDevice.setDeviceCode(String.format("RAD-PENDING-%03d", i));
            pendingDevice.setDeviceName(String.format("待激活辐射监测仪%03d", i));
            pendingDevice.setDeviceType(DeviceType.RADIATION_MONITOR);
            pendingDevice.setActivationStatus(DeviceActivationStatus.PENDING);
            pendingDevice.setStatus(DeviceStatus.OFFLINE);
            pendingDevice.setLocation("待安装区域C-" + i);
            pendingDevice.setDescription("待激活设备，用于测试批量激活功能");
            pendingDevice.setManufacturer("测试设备厂商");
            pendingDevice.setModel("RAD-Monitor-v2");
            pendingDevice.setSerialNumber(String.format("RAD-PEND-%03d", i));
            pendingDevice.setInstallDate(null);
            pendingDevice.setLastOnlineAt(null);
            pendingDevice.setCompany(company);

            Device savedPendingDevice = deviceRepository.save(pendingDevice);
            log.info("创建待激活设备: {} (ID: {})", savedPendingDevice.getDeviceCode(), savedPendingDevice.getId());
            createActivationCode(savedPendingDevice);
        }
    }

    /**
     * 初始化示例设备监测数据
     */
    private void initializeSampleDeviceData() {
        // 分别检查每个表，避免因一个表有数据而跳过其他表的初始化
        if (radiationDeviceDataRepository.count() == 0) {
            log.info("初始化辐射设备数据...");
            List<RadiationDeviceData> radiationDataList = createSampleRadiationData();
            List<RadiationDeviceData> savedRadiationData = radiationDeviceDataRepository.saveAll(radiationDataList);
            log.info("创建辐射设备示例数据: {} 条", savedRadiationData.size());
        } else {
            log.info("辐射设备数据已存在，跳过初始化");
        }

        if (environmentDeviceDataRepository.count() == 0) {
            log.info("初始化环境设备数据...");
            List<EnvironmentDeviceData> environmentDataList = createSampleEnvironmentData();
            List<EnvironmentDeviceData> savedEnvironmentData = environmentDeviceDataRepository.saveAll(environmentDataList);
            log.info("创建环境设备示例数据: {} 条", savedEnvironmentData.size());
        } else {
            log.info("环境设备数据已存在，跳过初始化");
        }
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
                    "{\"src\":\"RAD001\",\"msgtype\":\"data\",\"CPM\":%.0f,\"Batvolt\":3.7,\"time\":\"%s\"}",
                    cpm, recordTime.format(TIME_FORMATTER)
            ));
            data.setSrc(1); // 假设1表示设备源
            data.setMsgtype(1); // 假设1表示data类型
            data.setCpm(cpm);
            data.setBatvolt(3.7 + Math.random() * 0.3); // 3.7-4.0之间的随机电压
            data.setTime(recordTime.format(TIME_FORMATTER));
            data.setRecordTime(recordTime);
            data.setDataTrigger(1);
            data.setMulti(1);
            data.setWay(1);
            // 模拟GPS选择：假设BDS可用，使用北斗GPS
            data.setGpsType("BDS");
            data.setGpsLongitude(String.valueOf(116.4074 + Math.random() * 0.01)); // 北京经纬度附近
            data.setGpsLatitude(String.valueOf(39.9042 + Math.random() * 0.01));
            data.setGpsUtc(recordTime.format(TIME_FORMATTER));

            dataList.add(data);
        }

        // 为RAD002创建少量数据（离线设备）
        for (int i = 0; i < 5; i++) {
            LocalDateTime recordTime = baseTime.plusHours(i * 4);
            double cpm = 12 + Math.random() * 8;

            RadiationDeviceData data = new RadiationDeviceData();
            data.setDeviceCode("RAD002");
            data.setRawData(String.format(
                    "{\"src\":\"RAD002\",\"msgtype\":\"data\",\"CPM\":%.0f,\"Batvolt\":3.6,\"time\":\"%s\"}",
                    cpm, recordTime.format(TIME_FORMATTER)
            ));
            data.setSrc(2); // 假设2表示设备源
            data.setMsgtype(1); // 假设1表示data类型
            data.setCpm(cpm);
            data.setBatvolt(3.6);
            data.setTime(recordTime.format(TIME_FORMATTER));
            data.setRecordTime(recordTime);
            data.setDataTrigger(1);
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
                    "{\"src\":\"ENV001\",\"CPM\":%.0f,\"temperature\":%.1f,\"wetness\":%.1f,\"windspeed\":%.1f,\"total\":%.1f,\"battery\":%.1f}",
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

    /**
     * 生成唯一的激活码
     * 格式: EMS-{设备类型缩写}-{随机码8位}
     * 例如: EMS-RAD-X7K9P3M2
     */
    private String generateActivationCode(Device device) {
        String prefix = device.getDeviceType() == DeviceType.RADIATION_MONITOR ? "RAD" : "ENV";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String randomCode = sb.toString();
        return String.format("EMS-%s-%s", prefix, randomCode);
    }

    /**
     * 为设备创建激活码
     */
    private void createActivationCode(Device device) {
        String code;
        do {
            code = generateActivationCode(device);
        } while (activationCodeRepository.existsByCode(code));

        DeviceActivationCode activationCode = new DeviceActivationCode();
        activationCode.setDevice(device);
        activationCode.setCode(code);
        activationCode.setGeneratedAt(LocalDateTime.now());
        activationCode.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30天有效期
        activationCode.setStatus(ActivationCodeStatus.UNUSED);
        activationCodeRepository.save(activationCode);
        log.info("创建激活码: {} 用于设备 {}", activationCode.getCode(), device.getDeviceCode());
    }
}
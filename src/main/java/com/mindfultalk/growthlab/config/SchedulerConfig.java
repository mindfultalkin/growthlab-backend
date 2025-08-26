//package com.mindfultalk.growthlab.config;

//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.*;
//import org.slf4j.*;
//import com.mindfultalk.growthlab.service.*;

//@Configuration
//@EnableScheduling
//public class SchedulerConfig {
//    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
//    
//    @Autowired
//    private WeeklyReportService weeklyReportService;
//        
//    @Scheduled(cron = "0 00 10 * * Mon", zone = "Asia/Kolkata") // Every Monday at 10 AM
//    public void sendWeeklyEmails() {
//        System.out.println("Weekly email scheduler triggered at: " + java.time.LocalDateTime.now());
//        logger.info("Weekly email scheduler triggered at: {}", java.time.LocalDateTime.now());
//        try {
//            weeklyReportService.sendWeeklyReports();
//            logger.info("Weekly email process completed successfully.");
//        } catch (Exception e) {
//            logger.error("Error occurred while sending weekly emails: {}", e.getMessage(), e);
//        }
//    }
//}
//

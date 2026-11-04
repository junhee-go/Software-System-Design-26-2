package kr.ac.jnu.daymate.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationSettingTest {
    @Test
    void disabledSettingIsValidWithoutTimeOrCategories() {
        NotificationSetting setting = new NotificationSetting(false, null, false, false);

        assertTrue(setting.isValid());
    }

    @Test
    void enabledSettingRequiresAlertTime() {
        NotificationSetting setting = new NotificationSetting(true, null, true, false);

        assertFalse(setting.isValid());
    }

    @Test
    void enabledSettingRequiresAtLeastOneCategory() {
        NotificationSetting setting = new NotificationSetting(true, LocalTime.of(9, 0), false, false);

        assertFalse(setting.isValid());

        setting.setIncludeAssignment(true);
        assertTrue(setting.isValid());
    }
}

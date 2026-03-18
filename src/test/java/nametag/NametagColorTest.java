package nametag;

import main.io.github.itshaithamn.teamsandmore.nametag.NametagColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NametagColorTest {

    @Test
    void allValues_shouldHaveNonNullTextColor() {
        for (NametagColor color : NametagColor.values()) {
            assertNotNull(color.getTextColor());
        }
    }

    @Test
    void allValues_shouldHaveValidChatColor() {
        for (NametagColor color : NametagColor.values()) {
            String code = color.getChatColor();
            assertNotNull(code);
            assertTrue(code.startsWith("§"), color.name() + " should start with §");
            assertEquals(2, code.length(), color.name() + " should be exactly 2 chars");
        }
    }

    @Test
    void valueOf_shouldMatchEnumNames() {
        assertEquals(NametagColor.RED, NametagColor.valueOf("RED"));
        assertEquals(NametagColor.DARK_BLUE, NametagColor.valueOf("DARK_BLUE"));
    }

    @Test
    void enumCount_shouldBe14() {
        assertEquals(14, NametagColor.values().length);
    }

    @Test
    void differentColors_shouldHaveDifferentTextColors() {
        TextColor red = NametagColor.RED.getTextColor();
        TextColor blue = NametagColor.BLUE.getTextColor();
        assertNotEquals(red, blue);
    }
}
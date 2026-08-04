package com.integral.littlevecx.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class LittleVecXCrashBackup {

    private static final SimpleDateFormat FILE_DATE = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
    private static final int MAX_BACKUP_FILES = 30;

    private LittleVecXCrashBackup() {}

    public static void save(EntityPlayer player, LittlePreviews previews, String reason) {
        if (player == null || previews == null || previews.isEmptyIncludeChildren())
            return;

        try {
            File file = new File(getBackupDirectory(), buildFileName(player, previews));
            ItemStack backupStack = new ItemStack(Items.PAPER);
            LittlePreview.savePreview(previews.copy(), backupStack);

            NBTTagCompound root = new NBTTagCompound();
            root.setString("mod", LittleVecXMod.MODID);
            root.setString("format", "littlevecx_crash_backup");
            root.setString("created", FILE_DATE.format(new Date()));
            root.setString("reason", reason == null ? "unknown" : reason);
            root.setString("world", getWorldName(player));
            root.setInteger("dimension", player.dimension);
            root.setString("player", player.getName());
            if (previews.hasStructure())
                root.setString("structureName", previews.getStructureName());
            root.setTag("previewItem", backupStack.getTagCompound() == null ? new NBTTagCompound() : backupStack.getTagCompound().copy());

            try (FileOutputStream output = new FileOutputStream(file)) {
                CompressedStreamTools.writeCompressed(root, output);
            }
            trimOldBackups(file.getParentFile());

            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.crash_backup.saved", file.getName()), true);
        } catch (IOException | RuntimeException e) {
            player.sendStatusMessage(new TextComponentTranslation("message.littlevecx.crash_backup.failed"), true);
        }
    }

    private static File getBackupDirectory() throws IOException {
        File directory = new File(Minecraft.getMinecraft().gameDir, "littlevecx/crash_backup");
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Could not create LittleVecX crash backup directory: " + directory);
        return directory;
    }

    private static String buildFileName(EntityPlayer player, LittlePreviews previews) {
        String date = FILE_DATE.format(new Date());
        String name = previews.hasStructure() ? previews.getStructureName() : "";
        if (name == null || name.trim().isEmpty())
            name = getWorldName(player);
        if (name == null || name.trim().isEmpty())
            name = "unnamed";
        return date + "_" + sanitizeFileName(name) + ".nbt";
    }

    private static String getWorldName(EntityPlayer player) {
        if (player == null || player.world == null || player.world.getWorldInfo() == null)
            return "unknown_world";
        String name = player.world.getWorldInfo().getWorldName();
        return name == null || name.trim().isEmpty() ? "unknown_world" : name;
    }

    private static String sanitizeFileName(String value) {
        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|]+", "_").replaceAll("\\s+", "_");
        return sanitized.isEmpty() ? "unnamed" : sanitized;
    }

    private static void trimOldBackups(File directory) {
        File[] files = directory == null ? null : directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".nbt"));
        if (files == null || files.length <= MAX_BACKUP_FILES)
            return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = MAX_BACKUP_FILES; i < files.length; i++)
            if (files[i].isFile())
                files[i].delete();
    }
}

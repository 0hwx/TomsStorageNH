package com.hepdd.toms_storage.compat.mui.search;

import static com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortDirection;
import static com.hepdd.toms_storage.common.tile.TileEntityStorageTerminal.SortingTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.hepdd.toms_storage.client.SearchMatcher;
import com.hepdd.toms_storage.common.inventory.CachedItemStack;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class TerminalSearch {

    private final Int2ObjectMap<SearchDocument> searchCache = new Int2ObjectOpenHashMap<>();

    public List<Int2ObjectMap.Entry<CachedItemStack>> filter(Int2ObjectMap<CachedItemStack> source,
        SortingTypes sortType, SortDirection sortDirection, String searchText, EntityPlayer player) {
        String text = searchText == null ? "" : searchText;
        boolean modSearch = text.startsWith("@");
        boolean tooltipOnly = text.startsWith("#");
        String query = modSearch || tooltipOnly ? text.substring(1) : text;
        Pattern pattern = compilePattern(query);

        List<Int2ObjectMap.Entry<CachedItemStack>> result = new ArrayList<>();
        for (Int2ObjectMap.Entry<CachedItemStack> entry : source.int2ObjectEntrySet()) {
            if (pattern == null || matches(entry, pattern, modSearch, tooltipOnly, query, player)) {
                result.add(entry);
            }
        }

        boolean byName = sortType == SortingTypes.NAME;
        boolean reversed = sortDirection == SortDirection.DESCENDING;
        Comparator<Int2ObjectMap.Entry<CachedItemStack>> byDisplayName = Comparator
            .comparing(entry -> getSearchDocument(entry).displayName);
        Comparator<Int2ObjectMap.Entry<CachedItemStack>> cmp = byName ? byDisplayName
            : Comparator.<Int2ObjectMap.Entry<CachedItemStack>>comparingLong(
                entry -> entry.getValue()
                    .count())
                .reversed()
                .thenComparing(byDisplayName);
        result.sort(reversed ? cmp.reversed() : cmp);
        return result;
    }

    public void clear() {
        searchCache.clear();
    }

    public void forget(int itemId) {
        searchCache.remove(itemId);
    }

    private boolean matches(Int2ObjectMap.Entry<CachedItemStack> entry, Pattern pattern, boolean modSearch,
        boolean tooltipOnly, String query, EntityPlayer player) {
        SearchDocument document = getSearchDocument(entry);
        if (modSearch) return matchesMod(document, pattern);
        if (!tooltipOnly && SearchMatcher.matchesName(document.displayName, query, pattern)) return true;
        if (player == null) return false;
        if (document.tooltip == null) {
            document.tooltip = readTooltip(
                entry.getValue()
                    .stack(),
                player,
                document.displayName);
        }
        for (String line : document.tooltip) {
            if (SearchMatcher.matchesTooltip(line, query, pattern)) return true;
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private static List<String> readTooltip(ItemStack stack, EntityPlayer player, String displayName) {
        try {
            List<String> tooltip = stack.copy()
                .getTooltip(player, false);
            List<String> lines = new ArrayList<>(tooltip.size());
            String name = EnumChatFormatting.getTextWithoutFormattingCodes(displayName);
            for (int i = 0; i < tooltip.size(); i++) {
                String line = EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(i));
                if (line != null && !line.isEmpty() && (i != 0 || !line.equals(name))) lines.add(line);
            }
            return lines;
        } catch (RuntimeException | LinkageError ignored) {
            return Collections.emptyList();
        }
    }

    private boolean matchesMod(SearchDocument document, Pattern pattern) {
        for (String text : document.modTexts) {
            if (patternMatches(pattern, text)) return true;
        }
        return false;
    }

    private boolean patternMatches(Pattern pattern, String text) {
        if (text == null) return false;
        String bounded = text.length() > 256 ? text.substring(0, 256) : text;
        try {
            return pattern.matcher(bounded)
                .find();
        } catch (RuntimeException | StackOverflowError ignored) {
            return false;
        }
    }

    private Pattern compilePattern(String text) {
        if (text == null || text.isEmpty()) return null;
        if (isPotentiallyUnsafeRegex(text)) {
            return Pattern.compile(Pattern.quote(text.toLowerCase(Locale.ROOT)), Pattern.CASE_INSENSITIVE);
        }
        try {
            return Pattern.compile(text.toLowerCase(Locale.ROOT), Pattern.CASE_INSENSITIVE);
        } catch (RuntimeException ignored) {
            return Pattern.compile(Pattern.quote(text.toLowerCase(Locale.ROOT)), Pattern.CASE_INSENSITIVE);
        }
    }

    private boolean isPotentiallyUnsafeRegex(String text) {
        return text.contains("(?") || text.matches(".*\\\\[1-9].*") || text.matches(".*\\)[+*{].*");
    }

    private SearchDocument getSearchDocument(Int2ObjectMap.Entry<CachedItemStack> entry) {
        return searchCache.computeIfAbsent(
            entry.getIntKey(),
            ignored -> createSearchDocument(
                entry.getValue()
                    .stack()));
    }

    private SearchDocument createSearchDocument(ItemStack stack) {
        String displayName;
        try {
            displayName = stack.getDisplayName();
        } catch (RuntimeException exception) {
            displayName = "";
        }
        List<String> modTexts = new ArrayList<>();
        try {
            UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            if (identifier != null) {
                modTexts.add(identifier.modId);
                ModContainer mod = Loader.instance()
                    .getIndexedModList()
                    .get(identifier.modId);
                if (mod != null) modTexts.add(mod.getName());
            }
            modTexts.add(
                stack.getItem()
                    .getUnlocalizedName());
        } catch (RuntimeException ignored) {}
        return new SearchDocument(displayName, modTexts);
    }

    private static final class SearchDocument {

        private final String displayName;
        private final List<String> modTexts;
        private List<String> tooltip;

        private SearchDocument(String displayName, List<String> modTexts) {
            this.displayName = displayName;
            this.modTexts = modTexts;
        }
    }
}

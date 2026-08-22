package com.hepdd.toms_storage.compat.mui.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.hepdd.toms_storage.client.SearchMatcher;
import com.hepdd.toms_storage.common.sort.SortDirection;
import com.hepdd.toms_storage.common.sort.SortingTypes;
import com.hepdd.toms_storage.inventory.CachedItemStack;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public final class TerminalSearch {

    private final Map<CachedItemStack, SearchDocument> searchCache = new HashMap<>();

    public List<CachedItemStack> filter(List<CachedItemStack> source, SortingTypes sortType,
        SortDirection sortDirection, String searchText, EntityPlayer player) {
        String text = searchText == null ? "" : searchText;
        boolean modSearch = text.startsWith("@");
        Pattern pattern = compilePattern(modSearch ? text.substring(1) : text);

        List<CachedItemStack> result = new ArrayList<>();
        for (CachedItemStack stack : source) {
            if (stack == null || stack.stack() == null) continue;
            if (pattern == null
                || matches(getSearchDocument(stack.stack(), !modSearch, player), pattern, modSearch, text)) {
                result.add(stack);
            }
        }

        Map<CachedItemStack, String> names = new HashMap<>(result.size() * 2);
        for (CachedItemStack stack : result) {
            names.put(stack, stack.displayName());
        }
        boolean byName = sortType == SortingTypes.NAME;
        boolean reversed = sortDirection == SortDirection.DESCENDING;
        Comparator<CachedItemStack> cmp = byName ? Comparator.comparing(names::get)
            : Comparator.comparingLong(CachedItemStack::count)
                .reversed()
                .thenComparing(names::get);
        result.sort(reversed ? cmp.reversed() : cmp);
        return result;
    }

    private boolean matches(SearchDocument document, Pattern pattern, boolean modSearch, String text) {
        if (document == null) return false;
        if (pattern == null) return true;
        if (modSearch) return matchesMod(document, pattern);
        if (SearchMatcher.matchesName(document.displayName, text, pattern)) return true;
        if (document.tooltip != null) {
            for (String line : document.tooltip) {
                if (SearchMatcher.matchesTooltip(line, text, pattern)) return true;
            }
        }
        return false;
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

    private SearchDocument getSearchDocument(ItemStack stack, boolean includeTooltip, EntityPlayer player) {
        if (stack == null) return null;
        CachedItemStack key = new CachedItemStack(stack, 0);
        SearchDocument document = searchCache.get(key);
        if (document == null) {
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
            document = new SearchDocument(displayName, modTexts);
            searchCache.put(key, document);
        }
        if (includeTooltip && document.tooltip == null && player != null) {
            try {
                document.tooltip = new ArrayList<>(stack.getTooltip(player, false));
            } catch (RuntimeException exception) {
                document.tooltip = Collections.emptyList();
            }
        }
        return document;
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

/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.util;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.command.brigadier.argument.CustomArgumentParser;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class CommandUtil {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    public static boolean checkForCustomSuggestions(final CommandNode<?> rootSuggestion) {
        return rootSuggestion.getChildren()
                .stream()
                .filter(x -> x instanceof ArgumentCommandNode)
                .anyMatch(x -> ((ArgumentCommandNode<?, ?>) x).getCustomSuggestions() != null);
    }

    public static CompletableFuture<Suggestions> buildSuggestionsFromCompletions(
            final List<CommandCompletion> commandCompletions,
            final SuggestionsBuilder builder) {
        for (final CommandCompletion completion : commandCompletions) {
            final String s = completion.completion();
            final Message m = completion.tooltip().map(SpongeAdventure::asVanilla).orElse(null);
            if (CommandUtil.INTEGER_PATTERN.matcher(s).matches()) {
                try {
                    builder.suggest(Integer.parseInt(s), m);
                } catch (final NumberFormatException ex) {
                    builder.suggest(s, m);
                }
            } else {
                builder.suggest(s, m);
            }
        }
        return builder.buildFuture();
    }

    private CommandUtil() {
    }

}

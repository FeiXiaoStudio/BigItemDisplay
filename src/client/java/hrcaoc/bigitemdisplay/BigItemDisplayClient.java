package hrcaoc.bigitemdisplay;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class BigItemDisplayClient implements ClientModInitializer {
	public static Map<String, Boolean> renderRules = new HashMap<>(Map.ofEntries(
			Map.entry("showDisplay", false),
			Map.entry("isDisplayGlowing", false)
	));

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		CustomEntityRenderers.initialize();

		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) ->
				dispatcher.register(
						literal("bigitemdisplay").then(
								literal("show").then(
										argument("showDisplay", BoolArgumentType.bool())
										.executes(context -> {
											boolean showDisplay = BoolArgumentType.getBool(context, "showDisplay");
											renderRules.put("showDisplay", showDisplay);
											context.getSource().sendFeedback(Text.translatable("text.bigitemdisplay.showDisplay", showDisplay));
											return 1;
										})
								)
						)
				)
		);

		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) ->
				dispatcher.register(
						literal("bigitemdisplay").then(
								literal("glow").then(
										argument("isDisplayGlowing", BoolArgumentType.bool())
										.executes(context -> {
											boolean isDisplayGlowing = BoolArgumentType.getBool(context, "isDisplayGlowing");
											renderRules.put("isDisplayGlowing", isDisplayGlowing);
											context.getSource().sendFeedback(Text.translatable("text.bigitemdisplay.isDisplayGlowing", isDisplayGlowing));
											return 1;
										})
								)
						)
				)
		);
	}
}
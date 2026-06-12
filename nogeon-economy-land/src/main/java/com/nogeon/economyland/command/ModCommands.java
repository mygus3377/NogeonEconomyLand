package com.nogeon.economyland.command;

import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.HomeTeleportService;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerDisplayNameManager;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SocialClass;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.menu.WalletOpener;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.network.SyncMinerAbilityPacket;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.network.PacketDistributor;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("economy").executes(ctx -> openWallet(ctx.getSource())));
        dispatcher.register(literal("경제").executes(ctx -> openWallet(ctx.getSource())));

        dispatcher.register(literal("credit")
            .then(literal("balance").executes(ctx -> balance(ctx.getSource())))
            .then(literal("set")
                .requires(source -> source.hasPermission(2))
                .then(argument("amount", longArg(0)).executes(ctx -> setCredits(ctx.getSource(), ctx.getSource().getPlayerOrException(), getLong(ctx, "amount"))))
                .then(argument("player", player())
                    .then(argument("amount", longArg(0)).executes(ctx -> setCredits(ctx.getSource(), getPlayer(ctx, "player"), getLong(ctx, "amount"))))))
            .then(literal("grant")
                .requires(source -> source.hasPermission(2))
                .then(argument("amount", longArg(1)).executes(ctx -> grant(ctx.getSource(), getLong(ctx, "amount"))))));

        dispatcher.register(literal("job")
            .then(literal("select")
                .requires(source -> source.hasPermission(2))
                .then(argument("job", word()).executes(ctx -> selectJob(ctx.getSource(), getString(ctx, "job")))))
            .then(literal("level")
                .then(literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(argument("job", word())
                        .then(argument("level", IntegerArgumentType.integer(1))
                            .executes(ctx -> setJobLevel(ctx.getSource(), ctx.getSource().getPlayerOrException(), getString(ctx, "job"), IntegerArgumentType.getInteger(ctx, "level")))))
                    .then(argument("player", player())
                        .then(argument("job", word())
                            .then(argument("level", IntegerArgumentType.integer(1))
                                .executes(ctx -> setJobLevel(ctx.getSource(), getPlayer(ctx, "player"), getString(ctx, "job"), IntegerArgumentType.getInteger(ctx, "level"))))))))
            .then(literal("reset")
                .requires(source -> source.hasPermission(2))
                .then(argument("job", word())
                    .executes(ctx -> resetJob(ctx.getSource(), ctx.getSource().getPlayerOrException(), getString(ctx, "job"))))
                .then(argument("player", player())
                    .then(argument("job", word())
                        .executes(ctx -> resetJob(ctx.getSource(), getPlayer(ctx, "player"), getString(ctx, "job"))))))
            .then(literal("resetall")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> resetAllJobs(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(argument("player", player())
                    .executes(ctx -> resetAllJobs(ctx.getSource(), getPlayer(ctx, "player")))))
            .then(literal("info").executes(ctx -> jobInfo(ctx.getSource()))));

        dispatcher.register(literal("help")
            .then(literal("nogeon").executes(ctx -> helpNogeon(ctx.getSource()))));

        dispatcher.register(literal("socialclass")
            .then(literal("info").executes(ctx -> socialClassInfo(ctx.getSource())))
            .then(literal("set")
                .requires(source -> source.hasPermission(2))
                .then(argument("socialClass", word()).executes(ctx -> setSocialClass(ctx.getSource(), ctx.getSource().getPlayerOrException(), getString(ctx, "socialClass"))))
                .then(argument("player", player())
                    .then(argument("socialClass", word()).executes(ctx -> setSocialClass(ctx.getSource(), getPlayer(ctx, "player"), getString(ctx, "socialClass")))))));

        dispatcher.register(literal("home")
            .then(literal("save").then(argument("name", word()).executes(ctx -> saveHome(ctx.getSource(), getString(ctx, "name")))))
            .then(argument("name", word()).executes(ctx -> goHome(ctx.getSource(), getString(ctx, "name"))))
            .then(literal("list").executes(ctx -> listHomes(ctx.getSource())))
            .then(literal("delete").then(argument("name", word()).executes(ctx -> deleteHome(ctx.getSource(), getString(ctx, "name"))))));

        dispatcher.register(literal("economyadmin")
            .requires(source -> source.hasPermission(2))
            .then(literal("unstuckplayer")
                .then(argument("player", player()).executes(ctx -> unstuckPlayer(ctx.getSource(), getPlayer(ctx, "player")))))
            .then(literal("adminland")
                .then(literal("remove").executes(ctx -> removeAdminLand(ctx.getSource()))))
            .then(literal("jobs")
                .then(literal("resetall").executes(ctx -> resetAllSavedJobs(ctx.getSource()))))
            .then(literal("stop")
                .then(literal("patch").executes(ctx -> stopForPatch(ctx.getSource()))))
            .then(literal("generalshop")
                .then(literal("addhand")
                    .then(argument("price", longArg(1))
                        .then(argument("dailyLimit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                            .executes(ctx -> addHandToGeneralShop(ctx.getSource(), getLong(ctx, "price"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "dailyLimit"))))))));
    }

    private static int removeAdminLand(CommandSourceStack source) {
        if (EconomyState.get(source.getServer()).removeAdminLand()) {
            source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.adminland.remove_success"), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.adminland.remove_failed"));
            return 0;
        }
    }

    private static int unstuckPlayer(CommandSourceStack source, ServerPlayer target) {
        target.stopSleeping();
        target.closeContainer();
        target.deathTime = 0;
        target.hurtTime = 0;
        target.hurtDuration = 0;
        target.invulnerableTime = 0;
        target.clearFire();
        target.setTicksFrozen(0);
        target.setAirSupply(target.getMaxAirSupply());
        target.fallDistance = 0.0F;
        target.setDeltaMovement(Vec3.ZERO);
        target.removeAllEffects();
        target.setAbsorptionAmount(0.0F);
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20.0F);
        target.setHealth(Math.max(1.0F, Math.min(target.getMaxHealth(), Math.max(20.0F, target.getMaxHealth() * 0.5F))));
        target.teleportTo((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        target.inventoryMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal("Recovered stuck player: ").append(target.getDisplayName()), true);
        return 1;
    }

    private static int stopForPatch(CommandSourceStack source) {
        Component reason = Component.literal("\uc11c\ubc84 \ud328\uce58\uac00 \uc608\uc815\ub418\uc5b4 \uc7a0\uc2dc \uc885\ub8cc\ub429\ub2c8\ub2e4.\n\ub4dc\ub77c\uc774\ube0c\uc5d0\uc11c \uc0c8 \ubaa8\ub4dc\ud329\uc744 \ub2e4\uc2dc \ubc1b\uc544\uc8fc\uc138\uc694.");
        source.getServer().getPlayerList().broadcastSystemMessage(Component.literal("[NoGeon] \uc11c\ubc84 \ud328\uce58\ub97c \uc704\ud574 \uc7a0\uc2dc \uc885\ub8cc\ud569\ub2c8\ub2e4."), false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.connection.disconnect(reason);
        }
        source.getServer().halt(false);
        return 1;
    }

    private static int balance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerProfile profile = EconomyState.get(source.getServer()).profile(player.getUUID());
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.credit.balance", profile.credits()), false);
        return 1;
    }

    private static int grant(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(player.getUUID());
        profile.addCredits(amount);
        state.setDirty();
        SyncCreditsPacket.send(player, profile.credits());
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.credit.grant", amount, profile.credits()), false);
        return 1;
    }

    private static int setCredits(CommandSourceStack source, ServerPlayer target, long amount) {
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(target.getUUID());
        profile.setCredits(amount);
        state.setDirty();
        SyncCreditsPacket.send(target, profile.credits());
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.credit.set", target.getDisplayName(), profile.credits()), true);
        return 1;
    }

    private static int selectJob(CommandSourceStack source, String jobId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        JobType job = JobType.byId(jobId);
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(player.getUUID());
        JobType previousJob = profile.selectedJob();
        profile.setSelectedJob(job);
        resetMinerAbilitiesOnJobChange(player, profile, previousJob, job);
        state.setDirty();
        PlayerDisplayNameManager.refresh(player, profile);
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.job.select", Component.translatable("job.nogeon_economy_land." + job.id())), false);
        return 1;
    }

    private static void resetMinerAbilitiesOnJobChange(ServerPlayer player, PlayerProfile profile, JobType previousJob, JobType nextJob) {
        if (previousJob != JobType.MINER && nextJob != JobType.MINER) {
            return;
        }
        boolean bodyActive = false;
        int eyeRadius = 0;
        if (nextJob == JobType.MINER) {
            bodyActive = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN) > 0;
            int eyeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING);
            eyeRadius = eyeLevel > 0 ? Math.min(28, 8 + eyeLevel * 2) : 0;
        }
        profile.setMinerBodyActive(bodyActive);
        profile.setMinerEyeActive(false);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMinerAbilityPacket(bodyActive, false, eyeRadius));
    }

    private static int jobInfo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerProfile profile = EconomyState.get(source.getServer()).profile(player.getUUID());
        JobType job = profile.selectedJob();
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.job.info",
            Component.translatable("job.nogeon_economy_land." + job.id()),
            profile.job(job).level(),
            profile.job(job).exp(),
            profile.job(job).expToNextLevel(),
            profile.job(job).skillPoints()), false);
        return 1;
    }

    private static int setJobLevel(CommandSourceStack source, ServerPlayer target, String jobId, int level) {
        JobType job = JobType.byId(jobId);
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(target.getUUID());
        profile.job(job).setLevel(level);
        state.setDirty();
        PlayerDisplayNameManager.refresh(target, profile);
        source.sendSuccess(() -> Component.literal("Set ")
            .append(target.getDisplayName())
            .append(" ")
            .append(Component.translatable("job.nogeon_economy_land." + job.id()))
            .append(" level to " + profile.job(job).level() + "."), true);
        return 1;
    }

    private static int resetJob(CommandSourceStack source, ServerPlayer target, String jobId) {
        JobType job = JobType.byId(jobId);
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(target.getUUID());
        profile.job(job).reset();
        state.setDirty();
        PlayerDisplayNameManager.refresh(target, profile);
        source.sendSuccess(() -> Component.literal("Reset ")
            .append(target.getDisplayName())
            .append(" ")
            .append(Component.translatable("job.nogeon_economy_land." + job.id()))
            .append(" job progress."), true);
        return 1;
    }

    private static int resetAllJobs(CommandSourceStack source, ServerPlayer target) {
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(target.getUUID());
        profile.resetAllJobProgress();
        state.setDirty();
        PlayerDisplayNameManager.refresh(target, profile);
        source.sendSuccess(() -> Component.literal("Reset all job progress for ")
            .append(target.getDisplayName())
            .append("."), true);
        return 1;
    }

    private static int resetAllSavedJobs(CommandSourceStack source) {
        EconomyState state = EconomyState.get(source.getServer());
        int count = state.resetAllJobProgress();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            PlayerDisplayNameManager.refresh(player, state.profile(player.getUUID()));
        }
        source.sendSuccess(() -> Component.literal("Reset all job progress for " + count + " saved players."), true);
        return Math.max(1, count);
    }

    private static int helpNogeon(CommandSourceStack source) {
        sendHelp(source, "/economy", "Open the economy wallet.");
        sendHelp(source, "/credit balance", "Show your credit balance.");
        sendHelp(source, "/credit grant <amount>", "Admin: add credits to yourself.");
        sendHelp(source, "/credit set <amount>", "Admin: set your credits.");
        sendHelp(source, "/credit set <player> <amount>", "Admin: set another player's credits.");
        sendHelp(source, "/job info", "Show selected job level, exp, and skill points.");
        sendHelp(source, "/job select <farmer|fisher|miner|cook|hunter>", "Admin: change your selected job.");
        sendHelp(source, "/job level set <job> <level>", "Admin: set your job level and reset exp.");
        sendHelp(source, "/job level set <player> <job> <level>", "Admin: set another player's job level and reset exp.");
        sendHelp(source, "/job reset <job>", "Admin: reset your selected job progress for that job.");
        sendHelp(source, "/job reset <player> <job>", "Admin: reset another player's progress for that job.");
        sendHelp(source, "/job resetall", "Admin: reset all of your job levels, exp, skills, and active job toggles.");
        sendHelp(source, "/job resetall <player>", "Admin: reset all job progress for a player.");
        sendHelp(source, "/socialclass info", "Show your social class.");
        sendHelp(source, "/socialclass set <class>", "Admin: set your social class.");
        sendHelp(source, "/socialclass set <player> <class>", "Admin: set another player's social class.");
        sendHelp(source, "/home save <name>", "Save a home inside owned land.");
        sendHelp(source, "/home <name>", "Teleport to a saved home.");
        sendHelp(source, "/home list", "List saved homes.");
        sendHelp(source, "/home delete <name>", "Delete a saved home.");
        sendHelp(source, "/economyadmin unstuckplayer <player>", "Admin: recover a stuck player state.");
        sendHelp(source, "/economyadmin adminland remove", "Admin: remove admin spawn land.");
        sendHelp(source, "/economyadmin jobs resetall", "Admin: reset all job progress for every saved player.");
        sendHelp(source, "/economyadmin generalshop addhand <price> <dailyLimit>", "Admin: add held item to general shop.");
        return 1;
    }

    private static void sendHelp(CommandSourceStack source, String command, String description) {
        source.sendSuccess(() -> Component.literal(command + " - " + description), false);
    }

    private static int socialClassInfo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerProfile profile = EconomyState.get(source.getServer()).profile(player.getUUID());
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.social_class.info",
            Component.translatable(profile.socialClass().translationKey())), false);
        return 1;
    }

    private static int setSocialClass(CommandSourceStack source, ServerPlayer target, String socialClassId) {
        SocialClass socialClass = SocialClass.byId(socialClassId);
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(target.getUUID());
        profile.setSocialClass(socialClass);
        state.setDirty();
        PlayerDisplayNameManager.refresh(target, profile);
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.social_class.set",
            Component.translatable(socialClass.translationKey())), true);
        return 1;
    }

    private static int saveHome(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EconomyState state = EconomyState.get(source.getServer());
        if (!state.isHomeSaveAllowed(player.getUUID(), player.level().dimension(), player.blockPosition())) {
            source.sendFailure(Component.translatable("message.nogeon_economy_land.home.save_outside_land"));
            return 0;
        }
        PlayerProfile profile = state.profile(player.getUUID());
        if (!profile.homes().containsKey(name) && !profile.canAddHome()) {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.home.limit"));
            return 0;
        }
        profile.homes().put(name, HomeEntry.fromPlayer(name, player));
        state.setDirty();
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.home.saved", name), false);
        return 1;
    }

    private static int listHomes(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerProfile profile = EconomyState.get(source.getServer()).profile(player.getUUID());
        if (profile.homes().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.home.empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.home.list", String.join(", ", profile.homes().keySet())), false);
        return 1;
    }

    private static int deleteHome(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EconomyState state = EconomyState.get(source.getServer());
        PlayerProfile profile = state.profile(player.getUUID());
        if (profile.homes().remove(name) == null) {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.home.unknown", name));
            return 0;
        }
        state.setDirty();
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.home.deleted", name), false);
        return 1;
    }

    private static int goHome(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerProfile profile = EconomyState.get(source.getServer()).profile(player.getUUID());
        HomeEntry home = profile.homes().get(name);
        if (home == null) {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.home.unknown", name));
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(home.worldKey());
        if (level == null) {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.home.dimension_missing", name));
            return 0;
        }
        return HomeTeleportService.request(player, home) ? 1 : 0;
    }

    private static int openWallet(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WalletOpener.open(player);
        return 1;
    }

    private static int addHandToGeneralShop(CommandSourceStack source, long price, int dailyLimit) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("command.nogeon_economy_land.admin.empty_hand"));
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String entryId = itemId.replace(':', '_');
        EconomyState state = EconomyState.get(source.getServer());
        state.addOrReplaceGeneralShopEntry(new ShopEntry(entryId, stack.copy(), price, dailyLimit));
        source.sendSuccess(() -> Component.translatable("command.nogeon_economy_land.admin.shop_added",
            stack.getHoverName(), stack.getCount(), price, dailyLimit), true);
        return 1;
    }
}

package io.papermc.paper.connection;

import io.papermc.paper.adventure.PaperAdventure;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.ServerLinks;
import org.bukkit.craftbukkit.CraftServerLinks;

public abstract class PaperCommonConnection<T extends ServerCommonPacketListenerImpl> extends CommonCookieConnection implements PlayerCommonConnection {

    protected final T handle;

    public PaperCommonConnection(final T serverConfigurationPacketListenerImpl) {
        super(serverConfigurationPacketListenerImpl.connection);
        this.handle = serverConfigurationPacketListenerImpl;
    }

    @Override
    public void sendReportDetails(final Map<String, String> details) {
        this.handle.send(new ClientboundCustomReportDetailsPacket(details));
    }

    @Override
    public void sendLinks(final ServerLinks links) {
        this.handle.send(new ClientboundServerLinksPacket(((CraftServerLinks) links).getServerLinks().untrust()));
    }

    @Override
    public void transfer(final String host, final int port) {
        this.handle.send(new ClientboundTransferPacket(host, port));
    }

    @Override
    public void disconnect(final Component component) {
        this.handle.disconnect(PaperAdventure.asVanilla(component), DisconnectionReason.UNKNOWN);
    }

    @Override
    public boolean isTransferred() {
        return this.handle.isTransferred();
    }

    @Override
    public String getBrand() {
        return this.handle.playerBrand;
    }
}

package pan.alexander.tordnscrypt.di
/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import androidx.annotation.Keep
import dagger.Component
import pan.alexander.tordnscrypt.BootCompleteReceiver
import pan.alexander.tordnscrypt.MainActivity
import pan.alexander.tordnscrypt.TopFragment
import pan.alexander.tordnscrypt.backup.BackupFragment
import pan.alexander.tordnscrypt.backup.BackupHelper
import pan.alexander.tordnscrypt.di.logreader.LogReaderSubcomponent
import pan.alexander.tordnscrypt.dialogs.*

import pan.alexander.tordnscrypt.dnscrypt_fragment.DNSCryptFragmentReceiver
import pan.alexander.tordnscrypt.domain.preferences.PreferenceRepository
import pan.alexander.tordnscrypt.help.HelpActivity
import pan.alexander.tordnscrypt.help.HelpActivityReceiver
import pan.alexander.tordnscrypt.iptables.ModulesIptablesRules
import pan.alexander.tordnscrypt.iptables.Tethering
import pan.alexander.tordnscrypt.itpd_fragment.ITPDFragmentReceiver
import pan.alexander.tordnscrypt.main_fragment.MainFragment
import pan.alexander.tordnscrypt.modules.*
import pan.alexander.tordnscrypt.proxy.ProxyFragment
import pan.alexander.tordnscrypt.settings.*
import pan.alexander.tordnscrypt.settings.dnscrypt_relays.PreferencesDNSCryptRelays
import pan.alexander.tordnscrypt.settings.dnscrypt_servers.PreferencesDNSCryptServers
import pan.alexander.tordnscrypt.settings.dnscrypt_settings.PreferencesDNSFragment
import pan.alexander.tordnscrypt.settings.firewall.FirewallFragment
import pan.alexander.tordnscrypt.settings.tor_apps.UnlockTorAppsFragment
import pan.alexander.tordnscrypt.settings.tor_bridges.BridgeAdapter
import pan.alexander.tordnscrypt.settings.tor_bridges.GetNewBridges
import pan.alexander.tordnscrypt.settings.tor_bridges.PreferencesTorBridges
import pan.alexander.tordnscrypt.settings.tor_ips.UnlockTorIpsFragment
import pan.alexander.tordnscrypt.settings.tor_ips.UnlockTorIpsViewModel
import pan.alexander.tordnscrypt.settings.tor_preferences.PreferencesTorFragment
import pan.alexander.tordnscrypt.tiles.DNSCryptTileService
import pan.alexander.tordnscrypt.tiles.ITPDTileService
import pan.alexander.tordnscrypt.tiles.TorTileService
import pan.alexander.tordnscrypt.tor_fragment.TorFragmentReceiver
import pan.alexander.tordnscrypt.update.DownloadTask
import pan.alexander.tordnscrypt.update.UpdateService
import pan.alexander.tordnscrypt.utils.filemanager.FileManager
import pan.alexander.tordnscrypt.utils.integrity.Verifier
import pan.alexander.tordnscrypt.utils.web.TorRefreshIPsWork
import pan.alexander.tordnscrypt.vpn.service.ServiceVPN
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHandler
import javax.inject.Singleton

@Singleton
@Component(
    modules = [SharedPreferencesModule::class, RepositoryModule::class,
        DataSourcesModule::class, HelpersModule::class, CoroutinesModule::class,
        HandlerModule::class, ContextModule::class, InteractorsModule::class,
        AppSubcomponentModule::class]
)
@Keep
interface AppComponent {
    fun logReaderSubcomponent(): LogReaderSubcomponent.Factory

    fun getPathVars(): dagger.Lazy<PathVars>
    fun getPreferenceRepository(): dagger.Lazy<PreferenceRepository>

    fun inject(activity: MainActivity)
    fun inject(activity: SettingsActivity)
    fun inject(activity: HelpActivity)
    fun inject(fragment: TopFragment)
    fun inject(fragment: MainFragment)
    fun inject(fragment: PreferencesFastFragment)
    fun inject(fragment: UnlockTorAppsFragment)
    fun inject(fragment: PreferencesTorBridges)
    fun inject(fragment: UnlockTorIpsFragment)
    fun inject(fragment: PreferencesTorFragment)
    fun inject(fragment: ProxyFragment)
    fun inject(fragment: PreferencesDNSCryptServers)
    fun inject(fragment: FirewallFragment)
    fun inject(fragment: BackupFragment)
    fun inject(fragment: ConfigEditorFragment)
    fun inject(fragment: PreferencesCommonFragment)
    fun inject(fragment: PreferencesITPDFragment)
    fun inject(fragment: PreferencesDNSCryptRelays)
    fun inject(fragment: PreferencesDNSFragment)
    fun inject(fragment: UpdateModulesDialogFragment)
    fun inject(fragment: NotificationHelper)
    fun inject(service: ModulesService)
    fun inject(service: ServiceVPN)
    fun inject(service: UpdateService)
    fun inject(service: TorTileService)
    fun inject(service: DNSCryptTileService)
    fun inject(service: ITPDTileService)
    fun inject(receiver: BootCompleteReceiver)
    fun inject(receiver: DNSCryptFragmentReceiver)
    fun inject(receiver: TorFragmentReceiver)
    fun inject(receiver: ITPDFragmentReceiver)
    fun inject(receiver: ModulesBroadcastReceiver)
    fun inject(receiver: HelpActivityReceiver)
    fun inject(dialogFragment: RequestIgnoreBatteryOptimizationDialog)
    fun inject(dialogFragment: AskForceClose)
    fun inject(dialogFragment: SendCrashReport)
    fun inject(viewModel: UnlockTorIpsViewModel)
    fun inject(usageStatistic: UsageStatistic)
    fun inject(modulesKiller: ModulesKiller)
    fun inject(contextUIDUpdater: ContextUIDUpdater)
    fun inject(downloadTask: DownloadTask)
    fun inject(torRefreshIPsWork: TorRefreshIPsWork)
    fun inject(fileManager: FileManager)
    fun inject(verifier: Verifier)
    fun inject(bridgeAdapter: BridgeAdapter)
    fun inject(getNewBridges: GetNewBridges)
    fun inject(modulesStarterHelper: ModulesStarterHelper)
    fun inject(backupHelper: BackupHelper)
    fun inject(tethering: Tethering)
    fun inject(modulesIptablesRules: ModulesIptablesRules)
    fun inject(serviceVPNHandler: ServiceVPNHandler)
}
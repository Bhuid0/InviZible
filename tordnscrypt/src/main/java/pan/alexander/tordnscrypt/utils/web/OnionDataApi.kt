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

    Copyright 2019-2022 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt.utils.web

import pan.alexander.tordnscrypt.utils.Constants.TOR_BROWSER_USER_AGENT
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

private const val READ_TIMEOUT_SEC = 30
private const val CONNECT_TIMEOUT_SEC = 30
private const val USER_AGENT_PROPERTY = "User-Agent"
private const val REQUEST_METHOD_GET = "GET"

class OnionDataApi @Inject constructor() {

    fun requestOnionData(address: String, proxyAddress: String, proxyPort: Int): List<String> {

        val url = URL(address)

        val connection = if (proxyAddress.isNotBlank() && proxyPort != 0) {
            val proxy = Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress(
                    proxyAddress,
                    proxyPort
                )
            )

            url.openConnection(proxy) as HttpsURLConnection
        } else {
            url.openConnection() as HttpsURLConnection
        }

        connection.apply {
            requestMethod = REQUEST_METHOD_GET
            connectTimeout = CONNECT_TIMEOUT_SEC * 1000
            readTimeout = READ_TIMEOUT_SEC * 1000
            setRequestProperty(
                USER_AGENT_PROPERTY,
                TOR_BROWSER_USER_AGENT
            )
        }.connect()

        return connection.inputStream.bufferedReader().useLines {
            it.toList()
        }
    }
}
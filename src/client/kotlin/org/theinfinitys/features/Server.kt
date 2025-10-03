package org.theinfinitys.features

import org.theinfinitys.feature
import org.theinfinitys.features.server.ServerInfo

val server = listOf(feature("ServerInfo", ServerInfo(), "サーバーの情報を取得します。"))
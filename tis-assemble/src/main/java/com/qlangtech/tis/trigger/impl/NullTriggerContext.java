/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 */
package com.qlangtech.tis.trigger.impl;

import com.qlangtech.tis.trigger.ITriggerContext;

/**
 * @author 百岁（baisui@taobao.com）
 * @date 2014年12月12日下午3:49:48
 */
public class NullTriggerContext implements ITriggerContext {

    @Override
    public String getParam(String key) {

        return null;
    }

}

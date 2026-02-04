# 医生端Fragment重命名修复计划

## 📋 需要修改的文件

### 1. 患者管理Fragment（重命名为dt\_pt\_manage）

* ✅ 重命名Kotlin文件：`dt_pt_manegeFragment.kt` → `dt_pt_manageFragment.kt`

* ✅ 重命名XML文件：`fragment_dt_pt_manege.xml` → `fragment_dt_pt_manage.xml`

* ✅ 修改类名：`dt_homeFragment` → `dt_pt_manageFragment`

* ✅ 修改Binding类：`FragmentDtHomeBinding` → `FragmentDtPtManageBinding`

### 2. 主页Fragment（重命名为dt\_home）

* ✅ 重命名Kotlin文件：`dt_mainFragment.kt` → `dt_homeFragment.kt`

* ✅ 重命名XML文件：`fragment_dt_home.xml` → `fragment_dt_home.xml`（保持不变）

* ✅ 修改类名：`dt_mainFragment` → `dt_homeFragment`

* ✅ 修改Binding类：`FragmentDtMainBinding` → `FragmentDtHomeBinding`

### 3. 导航配置文件

* ✅ 更新 `dt_bot_nav.xml` 中的Fragment ID和类名引用

### 4. 菜单配置文件

* ✅ 更新 `dt_nav_menu.xml` 中的菜单项ID

### 5. Activity文件

* ✅ 更新 `DtHomeActivity.kt` 中的所有引用

### 6. 清理

* ✅ 清理build目录缓存

## 🔧 详细修改步骤

**步骤1**: 重命名患者管理相关文件

* 将 `dt_pt_manegeFragment.kt` 重命名为 `dt_pt_manageFragment.kt`

* 将 `fragment_dt_pt_manege.xml` 重命名为 `fragment_dt_pt_manage.xml`

* 修改类内部的所有引用

**步骤2**: 重命名主页相关文件

* 将 `dt_mainFragment.kt` 重命名为 `dt_homeFragment.kt`

* 修改类内部的所有引用

**步骤3**: 更新导航配置

* 将 `dt_mainFragment` ID改为 `dt_homeFragment`

* 将 `dt_homeFragment` ID改为 `dt_pt_manageFragment`

* 更新对应的类名和布局引用

**步骤4**: 更新菜单配置

* 将菜单项ID `dt_mainFragment` 改为 `dt_homeFragment`

* 将菜单项ID `dt_homeFragment` 改为 `dt_pt_manageFragment`

**步骤5**: 更新Activity引用

* 更新所有Fragment ID的引用

**步骤6**: 清理构建缓存

* 删除build目录，重新构建项目


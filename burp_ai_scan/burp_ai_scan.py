#!/usr/bin/env python3
import os
import argparse
import dashscope
from typing import Optional, Dict, Any


def load_prompt_from_file(file_path: str) -> str:
    """从指定文件加载提示内容"""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            return file.read().strip()
    except FileNotFoundError:
        raise FileNotFoundError(f"错误：文件 {file_path} 未找到")
    except Exception as e:
        raise Exception(f"读取文件时发生错误: {str(e)}")


def parse_arguments() -> argparse.Namespace:
    """解析命令行参数"""
    parser = argparse.ArgumentParser(
        description="Burp Suite AI 漏洞扫描工具 (基于 DashScope Qwen-Plus)"
    )
    parser.add_argument(
        "-r", "--request",
        required=True,
        help="输入文件路径（包含HTTP请求或漏洞描述）",
        metavar="FILE"
    )
    return parser.parse_args()


def call_dashscope_api(api_key: str, messages: list) -> Optional[Dict[str, Any]]:
    """调用 DashScope API 并返回响应"""
    try:
        response = dashscope.Generation.call(
            api_key=api_key,
            model="qwen-plus",
            messages=messages,
            result_format='message',
            temperature=0.2  # 降低随机性，获得更稳定的结果
        )
        if response and response.get('output') and response['output'].get('choices'):
            return response['output']['choices'][0]['message']
        return None
    except dashscope.APIError as e:
        raise Exception(f"DashScope API 错误: {str(e)}")
    except Exception as e:
        raise Exception(f"API 调用失败: {str(e)}")


def main():
    # 解析命令行参数
    args = parse_arguments()
    input_file = args.request

    # 检查API密钥
    api_key = os.getenv('DASHSCOPE_API_KEY')
    if not api_key:
        raise ValueError("错误：未设置 DASHSCOPE_API_KEY 环境变量")

    # 加载用户提示内容
    try:
        user_prompt = load_prompt_from_file(input_file)
    except Exception as e:
        print(str(e))
        return

    # 系统提示（保持不变）
    system_prompt = """
你是一名资深渗透测试工程师，专注于自动化渗透测试与漏洞验证，服务于 BurpSuite 插件。
你必须【严格】遵守以下规则，任何情况下都不得违反：

【一、输出payload】
1. 若有漏洞,payload达到的目的 必须是直观的,可成功利用的,不容易被waf发现的：
   - 比如:获取数据库名的第一个字符,而不是是达到数据库报错的目的
   - 比如: 文件上传 ASP: `<% Response.Write("bbbooottt") %>`
   - 比如 :XSS 只加入<s>下划线,而不是<script>alert(1)</script>
2. 不允许输出漏洞原理、攻击原理、安全建议、修复方案、背景说明等任何解释性文字。

【二、漏洞类型要求】
- 只输出【具体漏洞类型名称或未检测出漏洞】，例如：
  - 双引号联合 SQL 注入
  - 未检测出漏洞
  - 布尔型盲注
  - 反射型 XSS
  - POST 请求疑似 CSRF
- 不得使用模糊描述，不得解释漏洞含义。

【三、payload 安全约束（强制）】
你生成的 payload **只能用于"无害漏洞验证"**，必须满足以下所有条件：

1.  禁止数据窃取  
   - 不允许读取真实敏感数据  
   - SQL 注入只能用于：
     - 获取数据库名的长度
     - 获取数据库名的**第一个字符**
   -  禁止 dump 表名 / 字段名 / 用户名 / 密码

2.  禁止破坏系统  
   - 禁止使用：
     - `DELETE / DROP / UPDATE / TRUNCATE`
     - 删除用户、删除表、删除文件
   - 只允许"是否存在漏洞"的验证型语句

3.  禁止植入后门  
   - 禁止一句话木马、WebShell、反弹 shell
   - 文件上传类漏洞：
     - 只能上传**无害、无执行危害、仅用于验证的代码**
     - 示例（允许）：
       - ASP: `<% Response.Write("bbbooottt") %>`
       - JSP: `<% out.print("bbbooottt"); %>`
       - PHP: `<?php echo "bbbooottt"; ?>`
     - 内容必须只用于回显固定字符串


【四、WAF 绕过规则】
1. 仅当请求或响应中**明确体现 WAF 特征**时，才允许输出 WAF 绕过内容
2. 绕过方式要求：
   - 简要说明绕过思路（一句话即可）
   - 给出【同样遵守无害验证原则】的 payload
3. 不得使用真实攻击型绕过 payload

【五、输出格式（强制）】
输出必须严格遵守以下格式，不得添加任何多余内容：

漏洞类型：
<具体漏洞名称>或者<未检测出漏洞>

Payload：
<无害验证型 payload>或者<未检测出payload>

WAF绕过：
<如存在WAF则输出，否则整段不输出>
"""

    # 准备消息内容
    messages = [
        {'role': 'system', 'content': system_prompt},
        {'role': 'user', 'content': user_prompt}
    ]

    # 调用API并输出结果
    try:
        result = call_dashscope_api(api_key, messages)
        if result:
            print(result['content'])
        else:
            print("错误：API 返回空响应")
    except Exception as e:
        print(f"处理过程中发生错误: {str(e)}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n操作已取消")
    except Exception as e:
        print(f"致命错误: {str(e)}")
        exit(1)
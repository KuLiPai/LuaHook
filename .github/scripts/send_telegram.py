#!/usr/bin/env python3
import os
import sys
import html
import subprocess

def main():
    bot_token = os.environ.get("BOT_TOKEN")
    chat_id = os.environ.get("CHAT_ID")
    apk_path = os.environ.get("APK_PATH")
    short_sha = os.environ.get("SHORT_SHA", "")
    commit_sha = os.environ.get("COMMIT_SHA", "")
    author = os.environ.get("AUTHOR", "")
    repository = os.environ.get("REPOSITORY", "")
    raw_msg = os.environ.get("COMMIT_MSG", "")

    if not bot_token or not chat_id:
        print("⚠️ Telegram BOT_TOKEN or CHAT_ID not provided. Skipping notification.")
        sys.exit(0)

    if not apk_path or not os.path.exists(apk_path):
        print(f"❌ APK file not found at: {apk_path}")
        sys.exit(1)

    apk_filename = os.path.basename(apk_path)
    file_size_mb = os.path.getsize(apk_path) / (1024 * 1024)

    # Format commit message
    if not raw_msg or raw_msg.strip() == "":
        safe_msg = "手动触发构建 (workflow_dispatch)"
    else:
        # Take first 3 lines of commit message
        lines = [line.strip() for line in raw_msg.strip().splitlines() if line.strip()]
        safe_msg = "\n".join(lines[:3]) if lines else "无提交信息"

    escaped_msg = html.escape(safe_msg)
    escaped_author = html.escape(author)
    commit_url = f"https://github.com/{repository}/commit/{commit_sha}" if repository and commit_sha else ""

    caption = (
        f"📦 <b>Commit:</b> <a href=\"{commit_url}\">{short_sha}</a>\n"
        f"👤 <b>Author:</b> {escaped_author}\n"
        f"📊 <b>Size:</b> {file_size_mb:.2f} MB\n"
        f"📝 <b>Message:</b>\n{escaped_msg}"
    )

    # Telegram caption length limit is 1024 characters
    if len(caption) > 1000:
        available = 1000 - (len(caption) - len(escaped_msg)) - 3
        if available > 0:
            escaped_msg = escaped_msg[:available] + "..."
        else:
            escaped_msg = "..."
        caption = (
            f"📦 <b>Commit:</b> <a href=\"{commit_url}\">{short_sha}</a>\n"
            f"👤 <b>Author:</b> {escaped_author}\n"
            f"📊 <b>Size:</b> {file_size_mb:.2f} MB\n"
            f"📝 <b>Message:</b>\n{escaped_msg}"
        )

    print(f"🚀 Uploading {apk_filename} ({file_size_mb:.2f} MB) to Telegram chat {chat_id}...")

    url = f"https://api.telegram.org/bot{bot_token}/sendDocument"

    cmd = [
        "curl", "-s", "-S", "-w", "\nHTTP_STATUS:%{http_code}",
        "-F", f"chat_id={chat_id}",
        "-F", f"document=@{apk_path}",
        "-F", f"caption={caption}",
        "-F", "parse_mode=HTML",
        url
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    output = result.stdout.strip()
    print("Telegram Response:")
    print(output)

    if "HTTP_STATUS:200" not in output:
        print("⚠️ Failed to send APK to Telegram! Please check Bot Token, Chat ID, and Bot permissions in the chat/channel.")
        # Non-fatal so artifact upload in GitHub Actions is still marked successful
        sys.exit(0)
    else:
        print("✅ APK successfully sent to Telegram!")

if __name__ == "__main__":
    main()

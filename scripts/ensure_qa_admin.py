#!/usr/bin/env python3

import argparse
import subprocess
import sys

import bcrypt


def build_sql(username: str, email: str, display_name: str, password_hash: str) -> str:
    def quote(value: str) -> str:
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"

    username_sql = quote(username)
    email_sql = quote(email)
    display_name_sql = quote(display_name)
    password_hash_sql = quote(password_hash)

    return f"""
START TRANSACTION;

SET @qa_user_id := (
  SELECT id
  FROM users
  WHERE username = {username_sql} OR email = {email_sql}
  ORDER BY CASE WHEN username = {username_sql} THEN 0 ELSE 1 END
  LIMIT 1
);

UPDATE users
SET username = {username_sql},
    email = {email_sql},
    password_hash = {password_hash_sql},
    display_name = {display_name_sql},
    enabled = 1,
    deleted = 0,
    updated_by = 0
WHERE id = @qa_user_id;

INSERT INTO users (
  username,
  email,
  password_hash,
  display_name,
  enabled,
  created_by,
  updated_by,
  deleted
)
SELECT
  {username_sql},
  {email_sql},
  {password_hash_sql},
  {display_name_sql},
  1,
  0,
  0,
  0
FROM DUAL
WHERE @qa_user_id IS NULL;

SET @qa_user_id := COALESCE(@qa_user_id, LAST_INSERT_ID());

INSERT INTO user_role (user_id, role_code, created_by, updated_by, deleted)
VALUES (@qa_user_id, 'ADMIN', 0, 0, 0)
ON DUPLICATE KEY UPDATE
  deleted = 0,
  updated_by = 0;

COMMIT;

SELECT CONCAT(id, '\\t', username, '\\t', email, '\\t', display_name, '\\t', enabled)
FROM users
WHERE id = @qa_user_id;

SELECT role_code
FROM user_role
WHERE user_id = @qa_user_id AND deleted = 0
ORDER BY role_code;
"""


def run_mysql(sql: str, compose_dir: str, env_file: str) -> str:
    command = [
        "docker",
        "compose",
        "--env-file",
        env_file,
        "exec",
        "-T",
        "mysql",
        "sh",
        "-lc",
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -D "$MYSQL_DATABASE" -N -B',
    ]
    result = subprocess.run(
        command,
        cwd=compose_dir,
        input=sql,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        sys.stderr.write(result.stderr or result.stdout)
        raise SystemExit(result.returncode)
    return result.stdout.strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create or reset the local Docker QA admin account.")
    parser.add_argument("--compose-dir", default="deploy", help="Docker Compose directory. Default: deploy")
    parser.add_argument("--env-file", default=".env", help="Compose env file relative to --compose-dir. Default: .env")
    parser.add_argument("--username", default="admin.qa", help="QA admin username. Default: admin.qa")
    parser.add_argument("--email", default="admin.qa@ef.local", help="QA admin email. Default: admin.qa@ef.local")
    parser.add_argument("--display-name", default="QA Admin", help="QA admin display name. Default: QA Admin")
    parser.add_argument("--password", default="QaAdmin@123456", help="QA admin password. Default: QaAdmin@123456")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    password_hash = bcrypt.hashpw(args.password.encode("utf-8"), bcrypt.gensalt(rounds=10)).decode("utf-8")
    sql = build_sql(args.username, args.email, args.display_name, password_hash)
    output = run_mysql(sql, args.compose_dir, args.env_file)
    lines = [line for line in output.splitlines() if line]
    if not lines:
        print("admin.qa provisioning completed, but MySQL returned no summary rows.", file=sys.stderr)
        return 1

    user_summary = lines[0].replace("\\t", "\t").split("\t")
    if len(user_summary) < 5:
        print(f"Unexpected user summary row: {lines[0]!r}", file=sys.stderr)
        return 1
    roles = ", ".join(lines[1:]) if len(lines) > 1 else "none"
    print(f"QA admin ready: id={user_summary[0]} username={user_summary[1]} email={user_summary[2]}")
    print(f"display_name={user_summary[3]} enabled={user_summary[4]} roles={roles}")
    print(f"password={args.password}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

<?php
header('Content-Type: text/plain; charset=utf-8');
exec('sudo /usr/local/bin/tv-app-print.sh 2>&1', $out, $rc);
http_response_code($rc === 0 ? 200 : 500);
echo $rc === 0 ? "DONE\n" : "ERROR\n";
if ($rc !== 0) echo implode("\n", $out) . "\n";

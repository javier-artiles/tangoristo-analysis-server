# "scrapy shell" script to fetch JLPT lists 
# http://www.tanos.co.uk/jlpt/jlpt1/vocab/
# http://www.tanos.co.uk/jlpt/jlpt2/vocab/
# http://www.tanos.co.uk/jlpt/jlpt3/vocab/
# http://www.tanos.co.uk/jlpt/jlpt4/vocab/
# http://www.tanos.co.uk/jlpt/jlpt5/vocab/

import sys
reload(sys)
sys.setdefaultencoding("utf-8")

out = open("jlpt_n5.tsv", "w")
for row in response.xpath("""//*[@id="contentright"]/table[2]/tr[*]"""):
	kanji = row.xpath("./td[1]/a/text()").extract_first()
	kana = row.xpath("./td[2]/a/text()").extract_first()
	english = row.xpath("./td[3]/a/text()").extract_first()
	kanji = kanji if kanji else ""
	kana = kana if kana else ""
	english = english if english else ""
	out.write( "\t".join([kanji, kana, english]) + "\n")

out.close()


# https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists/Japanese
# https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists/Japanese10001-20000
out = open("wikipedia_ja_freq_2.lst", "w")
out.write('\n'.join(response.xpath("""//*[@id="mw-content-text"]/div/ol/li[*]//text()""").extract()))
out.close()
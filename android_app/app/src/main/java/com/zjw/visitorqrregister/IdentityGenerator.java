package com.zjw.visitorqrregister;

import java.security.SecureRandom;
import java.util.Set;

final class IdentityGenerator {
    private static final String[] SURNAMES = {"赵","钱","孙","李","周","吴","郑","王","冯","陈","褚","卫","蒋","沈","韩","杨","朱","秦","尤","许","何","吕","施","张","孔","曹","严","华","金","魏","陶","姜","谢","邹","喻","柏","水","窦","章","云","苏","潘","葛","奚","范","彭","郎","鲁","韦","昌","马","苗","凤","花","方","俞","任","袁","柳","鲍","史","唐","费","廉","岑","薛","雷","贺","倪","汤"};
    private static final String NAME_CHARS = "伟刚勇毅俊峰强军平保东文辉力明永健世广志义兴良海山仁波宁贵福生龙元全国胜学祥才发武新利清飞彬富顺信子杰涛昌成康星光天达安岩中茂进林有坚和彪博诚先敬震振壮会思群豪心邦承乐绍功松善厚庆磊民友裕河哲江超浩亮政谦亨奇固之轮翰朗伯宏言若鸣朋斌梁栋维启克伦翔旭鹏泽晨辰士以建家致树炎德行时泰盛雄琛钧冠策腾楠榕风航弘";
    private static final String[] PHONE_PREFIXES = {"130","131","132","133","135","136","137","138","139","150","151","152","155","156","157","158","159","166","173","175","176","177","178","180","181","182","183","185","186","187","188","189","191","198","199"};
    private final SecureRandom random = new SecureRandom();

    Identity next(Set<String> usedNames, Set<String> usedPhones) {
        String name = nextName(usedNames);
        String phone = nextPhone(usedPhones);
        return new Identity(name, phone);
    }

    private String nextName(Set<String> used) {
        for (int i = 0; i < 10_000; i++) {
            String name = SURNAMES[random.nextInt(SURNAMES.length)]
                    + NAME_CHARS.charAt(random.nextInt(NAME_CHARS.length()))
                    + NAME_CHARS.charAt(random.nextInt(NAME_CHARS.length()));
            if (!used.contains(name)) return name;
        }
        throw new IllegalStateException("name_pool_exhausted");
    }

    private String nextPhone(Set<String> used) {
        for (int i = 0; i < 100_000; i++) {
            StringBuilder sb = new StringBuilder(PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)]);
            for (int j = 0; j < 8; j++) sb.append(random.nextInt(10));
            String phone = sb.toString();
            if (!used.contains(phone)) return phone;
        }
        throw new IllegalStateException("phone_pool_exhausted");
    }

    static final class Identity {
        final String name;
        final String phone;
        Identity(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }
}
